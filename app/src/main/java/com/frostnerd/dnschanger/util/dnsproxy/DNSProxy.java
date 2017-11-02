package com.frostnerd.dnschanger.util.dnsproxy;

import android.content.Context;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;

import java.io.IOException;
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

    public abstract void run() throws InterruptedException, IOException, ErrnoException;

    public abstract void stop();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static DNSProxy createProxy(VpnService context, ParcelFileDescriptor parcelFileDescriptor,
                                       Set<IPPortPair> upstreamDNSServers, boolean resolveLocalRules, boolean queryLogging, boolean tcp) {
        if (tcp) {
            return new DNSTCPProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging, PreferencesAccessor.getTCPTimeout(context));
        } else {
            return new DNSUDPProxy(context, parcelFileDescriptor, upstreamDNSServers,
                    resolveLocalRules, queryLogging);
        }
    }
}
