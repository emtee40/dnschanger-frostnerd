package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.accessors.DNSResolver;
import com.frostnerd.dnschanger.database.accessors.QueryLogger;
import com.frostnerd.dnschanger.database.entities.DNSTLSConfiguration;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.threading.VPNRunnable;
import com.frostnerd.dnschanger.util.TLSSocketFactory;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

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
public class DNSTLSProxy extends DNSProxy{
    private static final String LOG_TAG = "[DNSTLSProxy]";
    private FileDescriptor interruptedDescriptor = null;
    private FileDescriptor blockingDescriptor = null;
    private ParcelFileDescriptor parcelFileDescriptor;
    private boolean shouldRun = true, resolveLocalRules, queryLogging;
    private final LinkedList<byte[]> writeToDevice = new LinkedList<>();
    private final static int MAX_WAITING_SOCKETS = 1000, SOCKET_TIMEOUT_MS = 10000, INSERT_CLEANUP_COUNT = 50;
    private DNSResolver resolver;
    private QueryLogger queryLogger;
    private VpnService vpnService;
    private final int timeout;
    private final Map<String, Socket> upstreamServers = new HashMap<>();
    private final Map<String, DNSTLSConfiguration> upstreamConfig = new HashMap<>();

    public DNSTLSProxy(VpnService context, ParcelFileDescriptor parcelFileDescriptor,
                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging, int timeout){
        LogFactory.writeMessage(context, LOG_TAG, "Creating the proxy...");
        if(parcelFileDescriptor == null)throw new IllegalStateException("The ParcelFileDescriptor passed to DNSUDPProxy is null.");
        if(context == null)throw new IllegalStateException("The DNSVPNService passed to DNSTCPProxy is null.");
        this.parcelFileDescriptor = parcelFileDescriptor;
        this.vpnService = context;
        LogFactory.writeMessage(context, LOG_TAG, "Parsing the upstream servers...");
        DNSTLSConfiguration config;
        for(IPPortPair pair: upstreamDNSServers){
            config = DatabaseHelper.getInstance(context).findTLSConfiguration(pair);
            if(config == null){
                LogFactory.writeMessage(context, LOG_TAG, "TLS configuration for server " + pair + " not found, omitting hostname check, using port " + pair.getPort());
                config = new DNSTLSConfiguration(pair.getPort(), new HashSet<>(Collections.singletonList(pair)));
            }
            upstreamConfig.put(pair.getAddress(), config);
        }
        LogFactory.writeMessage(context, LOG_TAG, "Upstream servers parsed to: " + this.upstreamConfig);
        this.resolveLocalRules = resolveLocalRules;
        this.queryLogging = queryLogging;
        if(queryLogging) {
            queryLogger = new QueryLogger(DatabaseHelper.getInstance(context));
            LogFactory.writeMessage(context, LOG_TAG, "Created the query logger.");
        }
        if(resolveLocalRules) {
            resolver = new DNSResolver(context);
            LogFactory.writeMessage(context, LOG_TAG, "Created the rule resolver.");
        }
        LogFactory.writeMessage(context, LOG_TAG, "Created the proxy.");
        this.timeout = timeout;
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
        while(shouldRun){
            StructPollfd structFd = new StructPollfd();
            structFd.fd = inputStream.getFD();
            structFd.events = (short) OsConstants.POLLIN;

            StructPollfd blockFd = new StructPollfd();
            blockFd.fd = blockingDescriptor;
            blockFd.events = (short) (OsConstants.POLLHUP | OsConstants.POLLERR);
            if(!writeToDevice.isEmpty())structFd.events = (short) (structFd.events | OsConstants.POLLOUT);

            StructPollfd[] polls = new StructPollfd[2 + upstreamServers.size()];
            polls[0] = structFd;
            polls[1] = blockFd;
            int index = 0;
            if(shouldRun){
                for(Socket socket: upstreamServers.values()){
                    StructPollfd pollingFd = polls[2 + index++] = new StructPollfd();
                    pollingFd.fd = ParcelFileDescriptor.fromSocket(socket).getFileDescriptor();
                    pollingFd.events = (short)OsConstants.POLLIN;
                }
                poll(polls, 5000);
            }
            if(blockFd.revents != 0){
                shouldRun = false;
                break;
            }

            index = 0;
            for(Socket s: upstreamServers.values()){
                if((polls[2 + index++].revents & OsConstants.POLLIN) != 0){
                    //TODO Read packet from socket
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
        InetAddress destination = VPNRunnable.addressRemap.get(packet.getHeader().getDstAddr().getHostAddress());
        if(destination == null || !upstreamConfig.containsKey(destination.getHostAddress()))return;
        UdpPacket udpPacket = (UdpPacket)packet.getPayload();
        if(udpPacket.getPayload() == null){
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, destination, 53);
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
                DatagramPacket outPacket = new DatagramPacket(payloadData, 0, payloadData.length, destination, 53);
                sendPacketToUpstreamDNSServer(outPacket, packet);
            }
        }
    }

    private void sendPacketToUpstreamDNSServer(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket ipPacket){
        try{
            Socket socket = establishConnection(outgoingPacket.getAddress().getHostAddress());
            outgoingPacket.setPort(upstreamConfig.get(outgoingPacket.getAddress().getHostAddress()).getPort());
            socket.connect(outgoingPacket.getSocketAddress(),timeout);
            byte[] data = ipPacket == null ? new byte[0] : outgoingPacket.getData();
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeShort(data.length);
            outputStream.write(data);
            outputStream.flush();
            /*if(ipPacket != null)futureSocketAnswers.put(socket, new PacketWrap(ipPacket));
            else{
                outputStream.close(); //Closes the associated socket
            }*/ //TODO
        }catch(IOException exception){
            if(!(exception instanceof SocketTimeoutException) && ipPacket != null){
                handleUpstreamDNSResponse(ipPacket, outgoingPacket.getData());
            }
        }
    }

    private void handleRawUpstreamDNSResponse(@NonNull Socket dnsSocket, @NonNull IpPacket parsedPacket){
        try {
            DataInputStream inputStream = new DataInputStream(dnsSocket.getInputStream());
            byte[] data = new byte[inputStream.readUnsignedShort()];
            inputStream.read(data);
            handleUpstreamDNSResponse(parsedPacket, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
    }

    private void closeSocket(@NonNull Socket socket){
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @NonNull
    private Socket establishConnection(String host) throws IOException {
        try{
            System.out.println("EStablishing connection to " + host);
            Socket socket = null;
            if(!upstreamServers.containsKey(host) || (socket = upstreamServers.get(host)) == null || socket.isClosed()){
                if(socket != null) closeSocket(socket);
            } else if(upstreamServers.containsKey(host)) {
                return socket;
            }
            DNSTLSConfiguration configuration = upstreamConfig.get(host);
            System.out.println("Creating socket to " + host + ":" + configuration.getPort());
            socket = getSocketFactory().createSocket(host, configuration.getPort());
            System.out.println("Socket create");
            SSLSession session = ((SSLSocket) socket).getSession();
            System.out.println("Got session");
            Certificate[] cchain = session.getPeerCertificates();
            System.out.println("The Certificates used by peer");
            for (int i = 0; i < cchain.length; i++) {
                System.out.println(((X509Certificate) cchain[i]).getSubjectDN());
            }
            vpnService.protect(socket); //The sent packets shouldn't be handled by this class
            upstreamServers.put(host, socket);
            return socket;
        } catch (Exception e) {
            e.printStackTrace(); //TODO remove
        }
        return null;
    }

    private SSLSocketFactory getSocketFactory(){
        try {
            return new TLSSocketFactory();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return (SSLSocketFactory) SSLSocketFactory.getDefault();
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
        synchronized (upstreamServers) {
            for (Map.Entry<String, Socket> entry : upstreamServers.entrySet()) {
                closeSocket(entry.getValue());
            }
            upstreamServers.clear();
        }
        writeToDevice.clear();
        upstreamConfig.clear();
        if(resolver != null) resolver.destroy();
        if(queryLogger != null) queryLogger.destroy();
        LogFactory.writeMessage(vpnService, LOG_TAG, "Everything was destructed.");
        parcelFileDescriptor = null;
        resolver = null;
        vpnService = null;
        queryLogger = null;
        interruptedDescriptor = blockingDescriptor = null;
    }

    private class UpstreamConfig {
        private String server;
        private DNSTLSConfiguration tlsConfig;
        private Socket upstream;
    }
}
