package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.entities.IPPortPair;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class DNSQueryUtil {

    public static void startDNSServerConnectivityCheck(@NonNull final Context context, @NonNull final Util.ConnectivityCheckCallback callback){
        runAsyncDNSQuery(PreferencesAccessor.isIPv4Enabled(context) ? PreferencesAccessor.Type.DNS1.getPair(context) :
                PreferencesAccessor.Type.DNS1_V6.getPair(context), "frostnerd.com", false, Type.A, DClass.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(Record[] response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void startDNSServerConnectivityCheck(@NonNull final IPPortPair server, @NonNull final Util.ConnectivityCheckCallback callback){
        runAsyncDNSQuery(server, "frostnerd.com", false, Type.A, DClass.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(Record[] response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void runAsyncDNSQuery(final IPPortPair server, final String query, final boolean tcp, final int type,
                                        final int dClass, final Util.DNSQueryResultListener resultListener, final int timeout){
        new Thread(){
            @Override
            public void run() {
                try {
                    Resolver resolver = new SimpleResolver(server.getAddress());
                    resolver.setPort(server.getPort());
                    resolver.setTCP(tcp);
                    resolver.setTimeout(timeout);
                    Lookup lookup = new Lookup(Name.fromString(query.endsWith(".") ? query : query + "."), type, dClass);
                    lookup.setResolver(resolver);
                    Record[] result = lookup.run();
                    if(result == null) throw new IllegalStateException("The result is null");
                    resultListener.onSuccess(result);
                } catch (IOException | IllegalStateException e) {
                    resultListener.onError(e);
                }
            }
        }.start();
    }

    public static Record[] runSyncDNSQuery(final IPPortPair server, final String query, final boolean tcp, final int type,
                                          final int dClass, final int timeout){
        try {
            Resolver resolver = new SimpleResolver(server.getAddress());
            resolver.setPort(server.getPort());
            resolver.setTCP(tcp);
            resolver.setTimeout(timeout);
            Lookup lookup = new Lookup(Name.fromString(query.endsWith(".") ? query : query + "."), type, dClass);
            lookup.setResolver(resolver);
            Record[] result = lookup.run();
            if(result == null) throw new IllegalStateException("The result is null");
            return result;
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }
}
