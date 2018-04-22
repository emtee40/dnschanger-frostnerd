package com.frostnerd.dnschanger.util.dnsproxy;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.PreferencesAccessor;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
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
        } else if(PreferencesAccessor.sendDNSOverTLS(context)){
            LogFactory.writeMessage(context, LOG_TAG, "Creating a TLS proxy");
            return new DNSTLSProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging, logUpstreamAnswers, PreferencesAccessor.getTCPTimeout(context));
        } else {
            LogFactory.writeMessage(context, LOG_TAG, "Creating an UDP proxy");
            return new DNSUDPProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging, logUpstreamAnswers);
        }
    }
}
