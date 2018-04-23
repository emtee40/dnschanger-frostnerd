package com.frostnerd.dnschanger.util.dnsquery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.source.NetworkDataSource;
import de.measite.minidns.util.MultipleIoException;

/*
  Modified by Daniel Wolf (frostnerd.com)
  Original author: https://github.com/MiniDNS/minidns

  Licensed under the WTFPL
 */
class TCPDataSource extends NetworkDataSource {
    @Override
    public DNSMessage query(DNSMessage message, InetAddress address, int port) throws IOException {
        List<IOException> ioExceptions = new ArrayList<>(2);
        DNSMessage dnsMessage = null;
        try {
            dnsMessage = queryTcp(message, address, port);
        } catch (IOException e) {
            ioExceptions.add(e);
        }

        if (dnsMessage != null && !dnsMessage.truncated) {
            return dnsMessage;
        }

        assert(dnsMessage == null || dnsMessage.truncated || ioExceptions.size() == 1);
        LOGGER.log(Level.FINE, "Fallback to TCP because {0}", new Object[] { dnsMessage != null ? "response is truncated" : ioExceptions.get(0) });

        try {
            dnsMessage = queryUdp(message, address, port);
        } catch (IOException e) {
            ioExceptions.add(e);
            MultipleIoException.throwIfRequired(ioExceptions);
        }

        return dnsMessage;
    }
}
