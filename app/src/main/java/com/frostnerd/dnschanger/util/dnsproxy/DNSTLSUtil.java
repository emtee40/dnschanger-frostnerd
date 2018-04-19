package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.entities.DNSTLSConfiguration;
import com.frostnerd.dnschanger.util.TLSSocketFactory;

import org.apache.http.conn.ssl.AbstractVerifier;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import de.measite.minidns.DNSMessage;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 *
 *
 * Look here:
 * https://tools.ietf.org/html/rfc7858
 */
public class DNSTLSUtil {
    private Map<String, Socket> upstreamServers = new LinkedHashMap<>();
    private Map<Socket, DataInputStream> inputStreamMap = new HashMap<>();
    private Map<String, DNSTLSConfiguration> upstreamConfig = new LinkedHashMap<>();
    private LinkedHashMap<DNSMessage, IpPacket> waitingQuestions = new LinkedHashMap<>();
    private VpnService service;
    private LinkedList<byte[]> responseData = new LinkedList<>();

    public DNSTLSUtil(VpnService service, Map<String, DNSTLSConfiguration> upstreamConfig) {
        this.upstreamConfig = upstreamConfig;
        this.service = service;
    }

    private Set<String> availableServers(){
        return upstreamConfig.keySet();
    }

    public void pollSockets(int maxPacketCount) throws IOException {
        try{
            outer: for(Socket socket: upstreamServers.values()) {
                int count = 0;
                DNSMessage message;
                do{
                    if((message = readDNSMessage(socket)) == null)continue outer;
                    System.out.println(message);
                    Map.Entry<DNSMessage, IpPacket> entry;
                    for(Iterator<Map.Entry<DNSMessage, IpPacket>> iterator = waitingQuestions.entrySet().iterator(); iterator.hasNext();){
                        entry = iterator.next();
                        if(entry.getKey().id == message.id){
                            handleUpstreamDNSResponse(entry.getValue(), message.asDatagram(null, 1).getData());
                            iterator.remove();
                            System.out.println("Send response.");
                        }
                    }
                }while(count++ <= maxPacketCount);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean canPollResponsedata(){
        return responseData.size() != 0;
    }

    public byte[] pollResponseData(){
        return responseData.poll();
    }

    private DNSMessage readDNSMessage(Socket socket) throws IOException {
        DataInputStream in = inputStreamMap.get(socket);
        byte[] lengthBytes = new byte[2];
        if(in.read(lengthBytes) <= 0)return null;
        int length = lengthBytes[0] + lengthBytes[1] << 8;
        byte[] data = new byte[length];
        in.read(data);
        return new DNSMessage(data);
    }

    public void sendPacket(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket packet) {
        try {
            if(outgoingPacket.getLength() == 0)sendPacket(outgoingPacket, packet, null);
            else sendPacket(outgoingPacket, packet, new DNSMessage(outgoingPacket.getData()));
        } catch(Exception exception) {
            if(!(exception instanceof SocketTimeoutException) && packet != null){
                handleUpstreamDNSResponse(packet, outgoingPacket.getData());
            }
        }
    }

    public void sendPacket(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket packet, @Nullable DNSMessage dnsMessage) {
        try {
            Socket socket = establishConnection(outgoingPacket.getAddress().getHostAddress());
            outgoingPacket.setPort(upstreamConfig.get(outgoingPacket.getAddress().getHostAddress()).getPort());
            byte[] data = outgoingPacket.getData();
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("DATA LEN: " + data.length);
            outputStream.writeShort(data.length);
            outputStream.write(data);
            outputStream.flush();
            if(packet != null && dnsMessage != null){
                waitingQuestions.put(dnsMessage, packet);
            }
            System.out.println("Send packet.");
        } catch(Exception exception) {
            System.out.println("Error: " + exception.getMessage());
            if(!(exception instanceof SocketTimeoutException) && packet != null){
                handleUpstreamDNSResponse(packet, outgoingPacket.getData());
            }
        }
    }

    @NonNull
    private Socket establishConnection(String host) throws IOException, CertificateException {
        Socket socket = null;
        if (!upstreamServers.containsKey(host) || (socket = upstreamServers.get(host)) == null) {
            if (socket != null) closeSocket(socket);
        } else if (upstreamServers.containsKey(host)) {
            return socket;
        }
        System.out.println("Establishing new connection");
        DNSTLSConfiguration configuration = upstreamConfig.get(host);
        socket = getSocketFactory().createSocket(host, configuration.getPort());
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(false);
        checkCertificate(((SSLSocket) socket).getSession(), configuration);
        service.protect(socket); //The sent packets shouldn't be handled by this class
        upstreamServers.put(host, socket);
        inputStreamMap.put(socket, new DataInputStream(socket.getInputStream()));
        return socket;
    }

    private void checkCertificate(@NonNull SSLSession session, @NonNull DNSTLSConfiguration tlsConfiguration) throws SSLPeerUnverifiedException,
            CertificateException {
        boolean hostFound = false;
        for (Certificate certificate : session.getPeerCertificates()) {
            ((X509Certificate) certificate).checkValidity();
            if(certificateIsFor(tlsConfiguration.getHostName(), (X509Certificate) certificate))hostFound = true;
        }
        if(!hostFound)
            throw new CertificateException("The configured host could not be found in the certification chain!");
    }

    private boolean certificateIsFor(String domain, X509Certificate certificate){
        Pattern pattern = Pattern.compile("^(\\*\\.)?" + domain + "$");
        Matcher matcher = pattern.matcher("");
        if(matcher.reset(certificate.getSubjectDN().toString()).matches())return true;
        String[] alt = AbstractVerifier.getDNSSubjectAlts(certificate);
        if(alt == null)return false;
        for (String s : alt) {
            if(matcher.reset(s).matches())return true;
        }
        return false;
    }

    private X509TrustManager getTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
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

    private void closeSocket(@NonNull Socket socket){
        try {
            socket.close();
        } catch (IOException ignored) {}
        inputStreamMap.remove(socket);
    }

    public void handleUpstreamDNSResponse(@NonNull IpPacket packet, @NonNull byte[] payloadData){
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
        responseData.add(packet.getRawData());
    }
}
