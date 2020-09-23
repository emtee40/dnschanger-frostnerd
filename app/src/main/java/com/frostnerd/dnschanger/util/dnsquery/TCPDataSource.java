package com.frostnerd.dnschanger.util.dnsquery;

import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsqueryresult.DnsQueryResult;
import org.minidns.dnsqueryresult.StandardDnsQueryResult;
import org.minidns.source.NetworkDataSource;
import org.minidns.util.MultipleIoException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/*
  Modified by Daniel Wolf (frostnerd.com)
  Original author: https://github.com/MiniDNS/minidns

  Licensed under the WTFPL
 */
class TCPDataSource extends NetworkDataSource {

    @Override
    public StandardDnsQueryResult query(DnsMessage message, InetAddress address, int port) throws IOException {
        List<IOException> ioExceptions = new ArrayList<>(2);
        DnsMessage dnsMessage = null;
        try {
            dnsMessage = queryTcp(message, address, port);
        } catch (IOException e) {
            ioExceptions.add(e);
        }

        if (dnsMessage != null && !dnsMessage.truncated) {
            return new StandardDnsQueryResult(address, port, DnsQueryResult.QueryMethod.tcp, dnsMessage.getQuestion().asQueryMessage(), dnsMessage);
        }

        LOGGER.log(Level.FINE, "Fallback to TCP because {0}", new Object[] { dnsMessage != null ? "response is truncated" : ioExceptions.get(0) });

        try {
            dnsMessage = queryUdp(message, address, port);
        } catch (IOException e) {
            ioExceptions.add(e);
            MultipleIoException.throwIfRequired(ioExceptions);
        }

        return new StandardDnsQueryResult(address, port, DnsQueryResult.QueryMethod.udp, dnsMessage == null ? null : dnsMessage.getQuestion().asQueryMessage(), dnsMessage);
    }
}
