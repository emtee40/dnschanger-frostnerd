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

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.pcap4j.packet.namednumber.IpVersion;
import org.pcap4j.util.ByteArrays;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class DNSTLSProxy extends DNSProxy{
    private static final String LOG_TAG = "[DNSTLSProxy]";
    private FileDescriptor interruptedDescriptor = null;
    private FileDescriptor blockingDescriptor = null;
    private ParcelFileDescriptor parcelFileDescriptor;
    private boolean shouldRun = true, resolveLocalRules, queryLogging;
    private DNSResolver resolver;
    private QueryLogger queryLogger;
    private VpnService vpnService;
    private DNSTLSUtil tlsUtil;

    public DNSTLSProxy(VpnService context, ParcelFileDescriptor parcelFileDescriptor,
                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging, int timeout){
        LogFactory.writeMessage(context, LOG_TAG, "Creating the proxy...");
        if(parcelFileDescriptor == null)throw new IllegalStateException("The ParcelFileDescriptor passed to DNSUDPProxy is null.");
        if(context == null)throw new IllegalStateException("The DNSVPNService passed to DNSTCPProxy is null.");
        this.parcelFileDescriptor = parcelFileDescriptor;
        this.vpnService = context;
        LogFactory.writeMessage(context, LOG_TAG, "Parsing the upstream servers...");
        DNSTLSConfiguration config;
        Map<String, DNSTLSConfiguration> upstreamConfig = new LinkedHashMap<>();
        for(IPPortPair pair: upstreamDNSServers){
            config = DatabaseHelper.getInstance(context).findTLSConfiguration(pair);
            if(config == null){
                LogFactory.writeMessage(context, LOG_TAG, "TLS configuration for server " + pair + " not found, omitting hostname check, using port " + pair.getPort());
                config = new DNSTLSConfiguration(pair.getPort(), new HashSet<>(Collections.singletonList(pair)));
            }
            upstreamConfig.put(pair.getAddress(), config);
        }
        LogFactory.writeMessage(context, LOG_TAG, "Upstream servers parsed to: " + upstreamConfig);
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
        this.tlsUtil = new DNSTLSUtil(vpnService, upstreamConfig);
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
            if(tlsUtil.canPollResponsedata())structFd.events = (short) (structFd.events | OsConstants.POLLOUT);

            StructPollfd[] polls = new StructPollfd[2];
            polls[0] = structFd;
            polls[1] = blockFd;
            tlsUtil.pollSockets(5);
            System.out.println("polled sockets. Polling Polls");
            poll(polls, 5000);
            System.out.println("Polled polls.");
            if(blockFd.revents != 0){
                shouldRun = false;
                System.out.println("Break.");
                break;
            }
            if(shouldRun && tlsUtil.canPollResponsedata()){
                System.out.println("Writing to device");
                outputStream.write(tlsUtil.pollResponseData());
                outputStream.flush();
            }
            if(shouldRun && (structFd.revents & OsConstants.POLLIN) != 0){
                System.out.println("Handling device packet");
                handleDeviceDNSPacket(inputStream, packet);
            }
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
        if(destination == null)return;
        UdpPacket udpPacket = (UdpPacket)packet.getPayload();
        if(udpPacket.getPayload() == null){
            DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0, destination, 53);
            sendPacketToUpstreamDNSServer(outPacket, null, null);
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
                if(builder != null)tlsUtil.handleUpstreamDNSResponse(packet, builder.build().toArray());
            }else{
                DatagramPacket outPacket = new DatagramPacket(payloadData, 0, payloadData.length, destination, 53);
                sendPacketToUpstreamDNSServer(outPacket, packet, dnsMsg);
            }
        }
    }

    private void sendPacketToUpstreamDNSServer(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket ipPacket, @Nullable DNSMessage dnsMessage){
        System.out.println("Sending to upstream");
        tlsUtil.sendPacket(outgoingPacket, ipPacket, dnsMessage);
    }

    @Override
    public void stop() {
        System.out.println("Stop.");
        LogFactory.writeMessage(vpnService, LOG_TAG, "Stopping the proxy");
        shouldRun = false;
        try {
            LogFactory.writeMessage(vpnService, LOG_TAG, "Closing the descriptors.");
            if(interruptedDescriptor != null) Os.close(interruptedDescriptor);
            if(blockingDescriptor != null) Os.close(blockingDescriptor);
        } catch (Exception ignored) {
            LogFactory.writeMessage(vpnService, LOG_TAG, "An error occurred when closing the descriptors: " + ignored.getMessage() + "(Cause: " + ignored.getCause() + ")");
        }
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
