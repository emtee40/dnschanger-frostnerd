package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.annotation.RequiresApi;
import android.system.ErrnoException;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

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
public abstract class DNSProxy {
    private static final String LOG_TAG = "[DNSPROXY]";
    public static final String IPV4_LOOPBACK_REPLACEMENT = "1.0.0.0",
    IPV6_LOOPBACK_REPLACEMENT = "fdce:b45b:8dd7:6e47:1:2:3:4";
    static InetAddress LOOPBACK_IPV4, LOOPBACK_IPV6;
    static{
        try {
            LOOPBACK_IPV4 = Inet4Address.getByName("127.0.0.1");
            LOOPBACK_IPV6 = Inet6Address.getByName("::1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public abstract void run() throws InterruptedException, IOException, ErrnoException;

    public abstract void stop();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DNSProxy createProxy(VpnService context, ParcelFileDescriptor parcelFileDescriptor,
                                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging, boolean logUpstreamAnswers) {
        LogFactory.writeMessage(context, LOG_TAG, "Creating a proxy with upstreamservers: " + upstreamDNSServers + " and file descriptor: " + parcelFileDescriptor);
        if (PreferencesAccessor.sendDNSOverTCP(context)) {
            LogFactory.writeMessage(context, LOG_TAG, "Creating a TCP proxy");
            return new DNSTCPProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging, logUpstreamAnswers, PreferencesAccessor.getTCPTimeout(context));
        } else {
            LogFactory.writeMessage(context, LOG_TAG, "Creating an UDP proxy");
            return new DNSUDPProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging, logUpstreamAnswers);
        }
    }
}
