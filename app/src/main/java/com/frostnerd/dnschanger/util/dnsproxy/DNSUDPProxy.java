package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.database.accessors.DNSResolver;
import com.frostnerd.dnschanger.database.accessors.QueryLogger;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

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

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 *
 * The code is in smaller parts based on dns66 (https://github.com/julian-klode/dns66) - but neither copied nor modified in its original state
 * Rather it was used as guidance.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DNSUDPProxy extends DNSProxy{
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
                eldest.getKey().close();
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
                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging){
        this.parcelFileDescriptor = parcelFileDescriptor;
        resolver = new DNSResolver(context);
        this.vpnService = context;
        for(IPPortPair pair: upstreamDNSServers){
            if(pair.getAddress() != null && !pair.getAddress().equals(""))this.upstreamServers.put(pair.getAddress(), pair.getPort());
        }
        this.resolveLocalRules = resolveLocalRules;
        this.queryLogging = queryLogging;
        if(queryLogging)queryLogger = new QueryLogger(Util.getDBHelper(context));
    }

    @Override
    public void run() throws IOException, ErrnoException {
        FileDescriptor[] pipes = Os.pipe();
        interruptedDescriptor = pipes[0];
        blockingDescriptor = pipes[1];
        if(!shouldRun)return;
        FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
        FileOutputStream outputStream = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
        byte[] packet = new byte[32767];
        while(shouldRun){
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
                    StructPollfd pollingFd = polls[2 + index++] = new StructPollfd();
                    pollingFd.fd = ParcelFileDescriptor.fromDatagramSocket(socket).getFileDescriptor();
                    pollingFd.events = (short)OsConstants.POLLIN;
                }
                Os.poll(polls, -1);
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
                    entry.getKey().close();
                }
            }
            if(shouldRun && (structFd.revents & OsConstants.POLLOUT) != 0)outputStream.write(writeToDevice.poll());
            if(shouldRun && (structFd.revents & OsConstants.POLLIN) != 0)handleDeviceDNSPacket(inputStream, packet);
        }
    }

    private void handleDeviceDNSPacket(InputStream inputStream, byte[] packetBytes) throws IOException{
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
        UdpPacket udpPacket = (UdpPacket)packet.getPayload();
        if(udpPacket.getPayload() == null){
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, destination, upstreamServers.get(destination.getHostAddress()));
            sendPacketToUpstreamDNSServer(outPacket, null);
        }else{
            byte[] payloadData = udpPacket.getPayload().getRawData();
            DNSMessage dnsMsg = new DNSMessage(payloadData);
            if(dnsMsg.getQuestion() == null)return;
            String query = dnsMsg.getQuestion().name.toString(), target;
            if(queryLogging)queryLogger.logQuery(query, dnsMsg.getQuestion().type == Record.TYPE.AAAA);
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
                DatagramPacket outPacket = new DatagramPacket(payloadData, 0, payloadData.length, destination, upstreamServers.get(destination.getHostAddress()));
                sendPacketToUpstreamDNSServer(outPacket, packet);
            }
        }
    }

    private void sendPacketToUpstreamDNSServer(DatagramPacket outgoingPacket, IpPacket ipPacket){
        try{
            DatagramSocket socket = new DatagramSocket();
            vpnService.protect(socket); //The sent packets shouldn't be handled by this class
            socket.send(outgoingPacket);
            if(ipPacket != null) futureSocketAnswers.put(socket, new PacketWrap(ipPacket));
            else socket.close();
        }catch(IOException exception){
            handleUpstreamDNSResponse(ipPacket, outgoingPacket.getData());
        }
    }

    private void handleRawUpstreamDNSResponse(DatagramSocket dnsSocket, IpPacket parsedPacket){
        try {
            byte[] datagramData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(datagramData, datagramData.length);
            dnsSocket.receive(replyPacket);
            handleUpstreamDNSResponse(parsedPacket, datagramData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleUpstreamDNSResponse(IpPacket packet, byte[] payloadData){
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
    }

    @Override
    public void stop() {
        shouldRun = false;
        try {
            if(interruptedDescriptor != null) Os.close(interruptedDescriptor);
            if(blockingDescriptor != null) Os.close(blockingDescriptor);
        } catch (Exception ignored) {
        }
        synchronized (futureSocketAnswers){
            for(DatagramSocket socket: futureSocketAnswers.keySet())socket.close();
            futureSocketAnswers.clear();
        }
        upstreamServers.clear();
        writeToDevice.clear();
        parcelFileDescriptor = null;
        resolver = null;
        vpnService = null;
        interruptedDescriptor = blockingDescriptor = null;
    }

    private class PacketWrap{
        private IpPacket packet;
        private long time;

        public PacketWrap(IpPacket packet) {
            this.packet = packet;
            this.time = System.currentTimeMillis();
        }

        public IpPacket getPacket() {
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
