package com.frostnerd.dnschanger.util.dnsquery;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.measite.minidns.AbstractDNSClient;
import de.measite.minidns.DNSClient;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.record.Data;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class Resolver {
    @Getter(lazy = true, value = AccessLevel.PRIVATE) private final AbstractDNSClient resolver = createResolver();
    @Getter(lazy = true, value = AccessLevel.PRIVATE) private final AbstractDNSClient tcpResolver = createTCPResolver();
    private InetAddress upstreamServer;
    private int timeout = -1;

    public Resolver(@NonNull String upstreamServer, @IntRange(from = 0) int timeout){
        setUpstreamAddress(upstreamServer);
        setTimeout(timeout);
    }

    public Resolver(@NonNull String upstreamServer){
        setUpstreamAddress(upstreamServer);
    }

    public void setUpstreamAddress(@NonNull String upstreamAddress) {
        try {
            upstreamServer = InetAddress.getByName(upstreamAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void setTimeout(@IntRange(from = 0) int timeout){
        if(resolver != null) getResolver().getDataSource().setTimeout(timeout);
        if(tcpResolver != null) getTcpResolver().getDataSource().setTimeout(timeout);
        this.timeout = timeout;
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, @IntRange(from = 1, to = 65535) int port) throws IOException {
        return resolve(name, type, false, port);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, @NonNull Record.CLASS clazz, @IntRange(from = 1, to = 65535) int port) throws IOException {
        return resolve(name, type, clazz, false, port);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, boolean tcp, @IntRange(from = 1, to = 65535) int port) throws IOException {
        return resolve(name, type, Record.CLASS.IN, tcp, port);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, @NonNull Record.CLASS clazz, boolean tcp, @IntRange(from = 1, to = 65535) int port) throws IOException {
        Question q = new Question(name, type, clazz);
        return resolve(q, tcp, port);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type) throws IOException {
        return resolve(name, type, false);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, @NonNull Record.CLASS clazz) throws IOException {
        return resolve(name, type, clazz, false);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, boolean tcp) throws IOException {
        return resolve(name, type, Record.CLASS.IN, tcp);
    }

    @NonNull
    public final <D extends Data> ResolverResult<D> resolve(@NonNull String name, @NonNull Record.TYPE type, @NonNull Record.CLASS clazz, boolean tcp) throws IOException {
        return resolve(name, type, clazz, tcp, 53);
    }

    @NonNull
    public <D extends Data> ResolverResult<D> resolve(@NonNull Question question, boolean tcp, @IntRange(from = 1, to = 65535) int port) throws IOException {
        DNSMessage dnsMessage;
        System.out.println("Sending question " + question + " to " + upstreamServer + ":" + port + " (tcp: " + tcp + ")");
        AbstractDNSClient resolver = tcp ? getTcpResolver() : getResolver();
        dnsMessage = resolver.query(question, upstreamServer, port);
        return new ResolverResult<>(question, dnsMessage, null);
    }

    @NonNull
    private AbstractDNSClient createResolver(){
        DNSClient client = new DNSClient();
        if(timeout != -1)client.getDataSource().setTimeout(timeout);
        return client;
    }

    @NonNull
    private AbstractDNSClient createTCPResolver(){
        DNSClient client = new DNSClient();
        client.setDataSource(new TCPDataSource());
        if(timeout != -1)client.getDataSource().setTimeout(timeout);
        return client;
    }

}
