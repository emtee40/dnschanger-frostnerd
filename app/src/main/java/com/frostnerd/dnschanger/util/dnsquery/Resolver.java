package com.frostnerd.dnschanger.util.dnsquery;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import org.minidns.AbstractDnsClient;
import org.minidns.DnsClient;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;
import org.minidns.record.Data;
import org.minidns.record.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


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
    private final AbstractDnsClient resolver = createResolver();
    private final AbstractDnsClient tcpResolver = createTCPResolver();
    private InetAddress upstreamServer;
    private int timeout = -1;

    public Resolver(@NonNull String upstreamServer, @IntRange(from = 1) int timeout){
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
        resolver.getDataSource().setTimeout(timeout);
        tcpResolver.getDataSource().setTimeout(timeout);
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
        DnsMessage dnsMessage;
        System.out.println("Sending question " + question + " to " + upstreamServer + ":" + port + " (tcp: " + tcp + ")");
        AbstractDnsClient resolver = tcp ? tcpResolver : this.resolver;
        dnsMessage = resolver.query(question, upstreamServer, port);
        return new ResolverResult<>(question, dnsMessage, null);
    }

    @NonNull
    private AbstractDnsClient createResolver(){
        DnsClient client = new DnsClient();
        if(timeout > 0)client.getDataSource().setTimeout(timeout);
        return client;
    }

    @NonNull
    private AbstractDnsClient createTCPResolver(){
        DnsClient client = new DnsClient();
        client.setDataSource(new TCPDataSource());
        if(timeout > 0)client.getDataSource().setTimeout(timeout);
        return client;
    }

}
