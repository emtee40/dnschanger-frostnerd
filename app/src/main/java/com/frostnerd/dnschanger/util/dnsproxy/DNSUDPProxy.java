package com.frostnerd.dnschanger.util.dnsproxy;

import android.annotation.TargetApi;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.accessors.DNSResolver;
import com.frostnerd.dnschanger.database.accessors.QueryLogger;
import com.frostnerd.dnschanger.database.entities.IPPortPair;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;
import de.measite.minidns.record.A;
import de.measite.minidns.record.AAAA;
import de.measite.minidns.record.Data;

/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DNSUDPProxy extends DNSProxy{
    private static final String LOG_TAG = "[DNSUDPProxy]";
    private FileDescriptor interruptedDescriptor = null;
    private FileDescriptor blockingDescriptor = null;
    private ParcelFileDescriptor parcelFileDescriptor;
    private boolean shouldRun = true, resolveLocalRules, queryLogging;
    private final LinkedList<byte[]> writeToDevice = new LinkedList<>();
    private final static int MAX_WAITING_SOCKETS = 1000, SOCKET_TIMEOUT_MS = 10000, INSERT_CLEANUP_COUNT = 50;
    private DNSResolver resolver;
    private QueryLogger queryLogger;
    private VpnService vpnService;
    private final HashMap<String, Integer> upstreamServers = new HashMap<>();
    private final LinkedHashMap<DatagramSocket, PacketWrap> futureSocketAnswers = new LinkedHashMap<DatagramSocket, PacketWrap>(){
        private int countSinceCleanup = 0;

        @Override
        protected boolean removeEldestEntry(Entry<DatagramSocket, PacketWrap> eldest) {
            if(size() > MAX_WAITING_SOCKETS){
                tryClose(eldest.getKey());
                return true;
            }
            return false;
        }

        @Override
        public PacketWrap put(DatagramSocket key, PacketWrap value) {
            if(countSinceCleanup++ >= INSERT_CLEANUP_COUNT){
                cleanupOldSockets();
                countSinceCleanup = 0;
            }
            return super.put(key, value);
        }

        private void cleanupOldSockets(){
            int max = size() >= MAX_WAITING_SOCKETS/3 ? size()/2 : size(), count = 0;
            Iterator<Entry<DatagramSocket, PacketWrap>> iterator = this.entrySet().iterator();
            Entry<DatagramSocket, PacketWrap> entry;
            while(iterator.hasNext() && count++ < max && shouldRun){
                entry = iterator.next();
                if(entry.getValue().getTimeDiff() >= SOCKET_TIMEOUT_MS)iterator.remove();
            }
        }
    };


    public DNSUDPProxy(VpnService context, ParcelFileDescriptor parcelFileDescriptor,
                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging, boolean logUpstreamAnswers){
        LogFactory.writeMessage(context, LOG_TAG, "Creating the proxy...");
        if(parcelFileDescriptor == null)throw new IllegalStateException("The ParcelFileDescriptor passed to DNSUDPProxy is null.");
        if(context == null)throw new IllegalStateException("The DNSVPNService passed to DNSUDPProxy is null.");
        this.parcelFileDescriptor = parcelFileDescriptor;
        this.vpnService = context;
        LogFactory.writeMessage(context, LOG_TAG, "Parsing the upstream servers...");
        for(IPPortPair pair: upstreamDNSServers){
            if(pair != IPPortPair.getEmptyPair() && !pair.getAddress().equals("")) this.upstreamServers.put(pair.getAddress(), pair.getPort());
        }
        LogFactory.writeMessage(context, LOG_TAG, "Upstream servers parsed to: " + this.upstreamServers);
        this.resolveLocalRules = resolveLocalRules;
        this.queryLogging = queryLogging;
        if(queryLogging) {
            queryLogger = new QueryLogger(DatabaseHelper.getInstance(context), logUpstreamAnswers);
            LogFactory.writeMessage(context, LOG_TAG, "Created the query logger.");
        }
        if(resolveLocalRules) {
            resolver = new DNSResolver(context);
            LogFactory.writeMessage(context, LOG_TAG, "Created the rule resolver.");
        }
        LogFactory.writeMessage(context, LOG_TAG, "Created the proxy.");
    }

    private void tryClose(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ex) {
            // Ignore
        }
    }

    @Override
    public void run() throws IOException, ErrnoException {
        LogFactory.writeMessage(vpnService, LOG_TAG, "Starting the proxy");
        if(!shouldRun){
            LogFactory.writeMessage(vpnService, LOG_TAG, "Not running as shouldRun is false.");
            return;
        }
        FileDescriptor[] pipes = Os.pipe();
        interruptedDescriptor = pipes[0];
        blockingDescriptor = pipes[1];
        FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
        FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
        byte[] packet = new byte[32767];
        LogFactory.writeMessage(vpnService, LOG_TAG, "Entering the while loop");
        outer: while(shouldRun){
            StructPollfd structFd = new StructPollfd();
            structFd.fd = inputStream.getFD();
            structFd.events = (short) OsConstants.POLLIN;

            StructPollfd blockFd = new StructPollfd();
            blockFd.fd = blockingDescriptor;
            blockFd.events = (short) (OsConstants.POLLHUP | OsConstants.POLLERR);
            if(!writeToDevice.isEmpty())structFd.events = (short) (structFd.events | OsConstants.POLLOUT);

            StructPollfd[] polls = new StructPollfd[2 + futureSocketAnswers.size()];
            polls[0] = structFd;
            polls[1] = blockFd;
            int index = 0;
            if(shouldRun){
                for(DatagramSocket socket: futureSocketAnswers.keySet()){
                    if(!shouldRun)break outer;
                    StructPollfd pollingFd = polls[2 + index++] = new StructPollfd();
                    pollingFd.fd = ParcelFileDescriptor.fromDatagramSocket(socket).getFileDescriptor();
                    pollingFd.events = (short)OsConstants.POLLIN;
                }
                poll(polls, 5000);
            }
            if(blockFd.revents != 0){
                shouldRun = false;
                break;
            }

            Iterator<Map.Entry<DatagramSocket, PacketWrap>> iterator = futureSocketAnswers.entrySet().iterator();
            Map.Entry<DatagramSocket, PacketWrap> entry;
            index = 0;
            while(iterator.hasNext() && shouldRun){
                entry = iterator.next();
                if((polls[index++ + 2].revents & OsConstants.POLLIN) != 0){
                    handleRawUpstreamDNSResponse(entry.getKey(), entry.getValue().getPacket());
                    iterator.remove();
                    tryClose(entry.getKey());
                }
            }
            if(shouldRun && (structFd.revents & OsConstants.POLLOUT) != 0)outputStream.write(writeToDevice.poll());
            if(shouldRun && (structFd.revents & OsConstants.POLLIN) != 0)handleDeviceDNSPacket(inputStream, packet);
        }
    }

    private int pollTries = 0;
    private void poll(@NonNull StructPollfd[] polls, int timeout) throws ErrnoException {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            pollTries++;
            try{
                Os.poll(polls, timeout/pollTries);
                pollTries = 0;
            } catch(ErrnoException ex){
                LogFactory.writeMessage(vpnService, LOG_TAG, "Polling failed with exception: " + ex.getMessage() + "(Cause: " + ex.getCause() + ")");
                if(ex.errno == OsConstants.EINTR && pollTries <= 5) poll(polls, timeout);
                else throw ex;
            }
        }else {
            Os.poll(polls, timeout);
        }
    }

    private void handleDeviceDNSPacket(@NonNull InputStream inputStream, @NonNull byte[] packetBytes) throws IOException{
        packetBytes = Arrays.copyOfRange(packetBytes, 0, inputStream.read(packetBytes));
        IpPacket packet;
        try {
            packet = (IpPacket) IpSelector.newPacket(packetBytes, 0, packetBytes.length);
            if(!(packet.getPayload() instanceof UdpPacket))return;
        } catch (Exception e) {
            return; //Packet from device isn't IP kind and thus is discarded
        }
        InetAddress destination = packet.getHeader().getDstAddr();
        if(destination == null || !upstreamServers.containsKey(destination.getHostAddress()))return;
        int port = upstreamServers.get(destination.getHostAddress());
        if(destination.getHostAddress().equals(IPV4_LOOPBACK_REPLACEMENT))destination = LOOPBACK_IPV4;
        else if(destination.getHostAddress().equals(IPV6_LOOPBACK_REPLACEMENT))destination = LOOPBACK_IPV6;
        UdpPacket udpPacket = (UdpPacket)packet.getPayload();
        if(udpPacket.getPayload() == null){
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, destination, port);
            sendPacketToUpstreamDNSServer(outPacket, null);
        }else{
            byte[] payloadData = udpPacket.getPayload().getRawData();
            DNSMessage dnsMsg = new DNSMessage(payloadData);
            if(dnsMsg.getQuestion() == null)return;
            String query = dnsMsg.getQuestion().name.toString(), target;
            if(queryLogging)queryLogger.logQuery(dnsMsg, dnsMsg.getQuestion().type == Record.TYPE.AAAA);
            LogFactory.writeMessage(vpnService, LOG_TAG, "Query from device: " + dnsMsg.getQuestion());
            if(resolveLocalRules && (target = resolver.resolve(query, dnsMsg.getQuestion().type == Record.TYPE.AAAA ,true)) != null){
                DNSMessage.Builder builder = null;
                if(dnsMsg.getQuestion().type == Record.TYPE.A){
                    builder = dnsMsg.asBuilder().setQrFlag(true).addAnswer(
                            new Record<Data>(query, Record.TYPE.A, 1, 64, new A(Inet4Address.getByName(target).getAddress())));
                }else if(dnsMsg.getQuestion().type == Record.TYPE.AAAA){
                    builder = dnsMsg.asBuilder().setQrFlag(true).addAnswer(
                            new Record<Data>(query, Record.TYPE.A, 1, 64, new AAAA(Inet6Address.getByName(target).getAddress())));
                }
                if(builder != null)handleUpstreamDNSResponse(packet, builder.build().toArray());
            }else{
                DatagramPacket outPacket = new DatagramPacket(payloadData, 0, payloadData.length, destination, port);
                sendPacketToUpstreamDNSServer(outPacket, packet);
            }
        }
    }

    private void sendPacketToUpstreamDNSServer(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket ipPacket){
        try{
            DatagramSocket socket = new DatagramSocket();
            vpnService.protect(socket); //The sent packets shouldn't be handled by this class
            socket.send(outgoingPacket);
            if(ipPacket != null) futureSocketAnswers.put(socket, new PacketWrap(ipPacket));
            else tryClose(socket);
        }catch(IOException exception){
            if(ipPacket != null)handleUpstreamDNSResponse(ipPacket, outgoingPacket.getData());
        }
    }

    private void handleRawUpstreamDNSResponse(@NonNull DatagramSocket dnsSocket, @NonNull IpPacket parsedPacket){
        try {
            byte[] datagramData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
            dnsSocket.receive(replyPacket);
            handleUpstreamDNSResponse(parsedPacket, datagramData);
        } catch (IOException ignored) {}
    }

    private void handleUpstreamDNSResponse(@NonNull IpPacket packet, @NonNull byte[] payloadData){
        UdpPacket dnsPacket = (UdpPacket) packet.getPayload();
        UdpPacket.Builder dnsPayloadBuilder = new UdpPacket.Builder(dnsPacket)
                .srcPort(dnsPacket.getHeader().getDstPort())
                .dstPort(dnsPacket.getHeader().getSrcPort())
                .srcAddr(packet.getHeader().getDstAddr())
                .dstAddr(packet.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(
                        new UnknownPacket.Builder().rawData(payloadData)
                );

        if(packet instanceof IpV4Packet){
            packet = new IpV4Packet.Builder((IpV4Packet)packet)
                    .srcAddr((Inet4Address)packet.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) packet.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(dnsPayloadBuilder)
                    .build();
        }else{
            packet = new IpV6Packet.Builder((IpV6Packet)packet)
                    .srcAddr((Inet6Address)packet.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) packet.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(dnsPayloadBuilder)
                    .build();
        }
        writeToDevice.add(packet.getRawData());
        if(queryLogger != null && queryLogger.logUpstreamAnswers()){
            try {
                queryLogger.logUpstreamAnswer(new DNSMessage(payloadData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        LogFactory.writeMessage(vpnService, LOG_TAG, "Stopping the proxy");
        shouldRun = false;
        try {
            LogFactory.writeMessage(vpnService, LOG_TAG, "Closing the descriptors.");
            if(interruptedDescriptor != null) Os.close(interruptedDescriptor);
            if(blockingDescriptor != null) Os.close(blockingDescriptor);
        } catch (Exception ignored) {
            LogFactory.writeMessage(vpnService, LOG_TAG, "An error occurred when closing the descriptors: " + ignored.getMessage() + "(Cause: " + ignored.getCause() + ")");
        }
        synchronized (futureSocketAnswers){
            for(Map.Entry<DatagramSocket, PacketWrap> entry: futureSocketAnswers.entrySet()){
                tryClose(entry.getKey());
                entry.getValue().packet = null;
            }
            futureSocketAnswers.clear();
        }
        upstreamServers.clear();
        writeToDevice.clear();

        if(resolver != null) resolver.destroy();
        if(queryLogger != null) queryLogger.destroy();
        LogFactory.writeMessage(vpnService, LOG_TAG, "Everything was destructed.");
        parcelFileDescriptor = null;
        resolver = null;
        vpnService = null;
        queryLogger = null;
        interruptedDescriptor = blockingDescriptor = null;
    }

    private class PacketWrap{
        @NonNull private IpPacket packet;
        private final long time;

        PacketWrap(@NonNull IpPacket packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        @NonNull IpPacket getPacket() {
            return packet;
        }

        public long getTime() {
            return time;
        }

        public long getTimeDiff(){
            return System.currentTimeMillis() - time;
        }
    }
}
