package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.accessors.QueryLogger;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 * <p>
 * <p>
 * Look here:
 * https://tools.ietf.org/html/rfc7858
 */
public class DNSTLSUtil {
    private final Map<String, Socket> upstreamServers = new LinkedHashMap<>();
    private final Map<String, DNSTLSConfiguration> upstreamConfig;
    private final LinkedHashMap<DNSMessage, IpPacket> waitingQuestions = new LinkedHashMap<>();
    private final LinkedList<byte[]> responseData = new LinkedList<>();
    private final List<PastAnswer> history = new ArrayList<>();
    private VpnService service;
    private QueryLogger queryLogger;
    private final static int MAX_HISTORY_COMPARISONS = 600;
    private Handler handler;
    private boolean cacheResponses = true;
    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    Set<Socket> sockets = new HashSet<>(upstreamServers.values());
                    outer:
                    for (Socket socket : sockets) {
                        int count = 0;
                        DNSMessage message;
                        do {
                            if ((message = readDNSMessage(socket)) == null) continue outer;
                            Map.Entry<DNSMessage, IpPacket> entry;
                            synchronized (waitingQuestions) {
                                for (Iterator<Map.Entry<DNSMessage, IpPacket>> iterator = waitingQuestions.entrySet().iterator(); iterator.hasNext(); ) {
                                    entry = iterator.next();
                                    if (entry.getKey().id == message.id) {
                                        handleUpstreamDNSResponse(entry.getValue(), message.asDatagram(null, 1).getData());
                                        if(cacheResponses)history.add(new PastAnswer(message, message.getQuestion()));
                                        iterator.remove();
                                    }
                                }
                            }
                        } while (count++ <= 5);
                    }
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    };

    public DNSTLSUtil(VpnService service, Map<String, DNSTLSConfiguration> upstreamConfig) {
        this.upstreamConfig = upstreamConfig;
        this.service = service;
        new Thread(pollRunnable).start();
    }

    public void setQueryLogger(QueryLogger queryLogger) {
        this.queryLogger = queryLogger;
    }

    public boolean canPollResponsedata() {
        return responseData.size() != 0;
    }

    public byte[] pollResponseData() {
        synchronized (responseData) {
            return responseData.poll();
        }
    }

    private DNSMessage readDNSMessage(Socket socket) throws IOException {
        DataInputStream in = new DataInputStream(socket.getInputStream());
        byte[] lengthBytes = new byte[2];
        if (in.read(lengthBytes) <= 0) return null;
        int length = (lengthBytes[0] & 0xFF) + (lengthBytes[1] & 0xFF) << 8;
        byte[] data = new byte[length];
        in.read(data);
        return new DNSMessage(data);
    }

    @Nullable
    private DNSMessage getOldAnswer(@NonNull DNSMessage currentMessage) {
        if(!cacheResponses)return null;
        if (history.size() != 0) {
            int comparisons = 0;
            PastAnswer found = null;
            Question currentQuestion = currentMessage.getQuestion();
            for (PastAnswer pastAnswer : history) {
                if (++comparisons > MAX_HISTORY_COMPARISONS) break;
                if (currentQuestion.name.compareTo(pastAnswer.oldQuestion.name) != 0) continue;
                if (currentQuestion.type != pastAnswer.oldQuestion.type && currentQuestion.type != Record.TYPE.ANY)
                    continue;
                if (currentQuestion.clazz != pastAnswer.oldQuestion.clazz && currentQuestion.clazz != Record.CLASS.ANY)
                    continue;
                found = pastAnswer;
                break;
            }
            if (found != null) {
                System.out.println("Found an old answer for " + currentMessage);
                found.futureHits++;
                Collections.sort(history);
            }
        }
        return null;
    }

    public void sendPacket(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket packet) {
        try {
            if (outgoingPacket.getLength() == 0) sendPacket(outgoingPacket, packet, null);
            else sendPacket(outgoingPacket, packet, new DNSMessage(outgoingPacket.getData()));
        } catch (Exception exception) {
            if (!(exception instanceof SocketTimeoutException) && packet != null) {
                handleUpstreamDNSResponse(packet, outgoingPacket.getData());
            }
        }
    }

    private int sendTries = 0;

    public void sendPacket(@NonNull DatagramPacket outgoingPacket, @Nullable IpPacket packet, @Nullable DNSMessage dnsMessage) {
        Socket socket = null;
        String host = outgoingPacket.getAddress().getHostAddress();
        try {
            outgoingPacket.setPort(upstreamConfig.get(outgoingPacket.getAddress().getHostAddress()).getPort());
            byte[] data = outgoingPacket.getData();
            DNSMessage message = new DNSMessage(data);
            if (packet != null && dnsMessage != null) {
                if ((message = getOldAnswer(message)) != null) {
                    handleUpstreamDNSResponse(packet, message.asDatagram(null, 1).getData());
                    return;
                }
            }
            sendTries++;
            socket = establishConnection(host);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeShort(data.length);
            outputStream.write(data);
            outputStream.flush();
            synchronized (waitingQuestions) {
                waitingQuestions.put(dnsMessage, packet);
            }
            sendTries = 0;
        } catch (Exception exception) {
            if (exception instanceof SSLException) {
                if (sendTries <= 5) {
                    System.out.println("Retrying sending. Tries: " + sendTries);
                    if (socket != null) closeSocket(socket);
                    upstreamServers.remove(host);
                    sendPacket(outgoingPacket, packet, dnsMessage);
                } else throw new RuntimeException(exception);
            } else if (!(exception instanceof SocketTimeoutException) && packet != null) {
                handleUpstreamDNSResponse(packet, outgoingPacket.getData());
            }
        }
    }

    @NonNull
    private Socket establishConnection(String host) throws IOException, CertificateException {
        if (upstreamServers.containsKey(host)) {
            return upstreamServers.get(host);
        }
        synchronized (upstreamServers) {
            DNSTLSConfiguration configuration = upstreamConfig.get(host);
            Socket socket = getSocketFactory().createSocket(host, configuration.getPort());
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(false);
            checkCertificate(((SSLSocket) socket).getSession(), configuration);
            service.protect(socket); //The sent packets shouldn't be handled by this class
            upstreamServers.put(host, socket);
            return socket;
        }
    }

    private void checkCertificate(@NonNull SSLSession session, @NonNull DNSTLSConfiguration tlsConfiguration) throws SSLPeerUnverifiedException,
            CertificateException {
        boolean hostFound = false;
        for (Certificate certificate : session.getPeerCertificates()) {
            ((X509Certificate) certificate).checkValidity();
            if (certificateIsFor(tlsConfiguration.getHostName(), (X509Certificate) certificate))
                hostFound = true;
        }
        if (!hostFound)
            throw new CertificateException("The configured host could not be found in the certification chain!");
    }

    private boolean certificateIsFor(String domain, X509Certificate certificate) {
        if (domain == null || domain.equals("")) return true;
        Pattern pattern = Pattern.compile("^(\\*\\.)?" + domain + "$");
        Matcher matcher = pattern.matcher("");
        if (matcher.reset(certificate.getSubjectDN().toString()).matches()) return true;
        String[] alt = AbstractVerifier.getDNSSubjectAlts(certificate);
        if (alt == null) return false;
        for (String s : alt) {
            if (matcher.reset(s).matches()) return true;
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

    private SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void closeSocket(@NonNull Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public void handleUpstreamDNSResponse(@NonNull IpPacket packet, @NonNull byte[] payloadData) {
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

        if (packet instanceof IpV4Packet) {
            packet = new IpV4Packet.Builder((IpV4Packet) packet)
                    .srcAddr((Inet4Address) packet.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) packet.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(dnsPayloadBuilder)
                    .build();
        } else {
            packet = new IpV6Packet.Builder((IpV6Packet) packet)
                    .srcAddr((Inet6Address) packet.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) packet.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(dnsPayloadBuilder)
                    .build();
        }
        synchronized (responseData) {
            responseData.add(packet.getRawData());
        }
        if (queryLogger != null && queryLogger.logUpstreamAnswers()) {
            try {
                queryLogger.logUpstreamAnswer(new DNSMessage(payloadData));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final class PastAnswer implements Comparable<PastAnswer> {
        private int futureHits = 0;
        @NonNull
        private DNSMessage oldAnswer;
        @NonNull
        private Question oldQuestion;

        public PastAnswer(@NonNull DNSMessage oldAnswer, @NonNull Question oldQuestion) {
            this.oldAnswer = oldAnswer;
            this.oldQuestion = oldQuestion;
        }

        @Override
        public int compareTo(@NonNull PastAnswer o) {
            return futureHits - o.futureHits;
        }
    }
}
