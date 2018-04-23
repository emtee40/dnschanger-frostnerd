package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.dnsquery.Resolver;
import com.frostnerd.dnschanger.util.dnsquery.ResolverResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.measite.minidns.Record;
import de.measite.minidns.record.Data;

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
                PreferencesAccessor.Type.DNS1_V6.getPair(context), "frostnerd.com", false, Record.TYPE.A, Record.CLASS.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(List<Record<? extends Data>> response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void startDNSServerConnectivityCheck(@NonNull final IPPortPair server, @NonNull final Util.ConnectivityCheckCallback callback){
        if(server == null)return;
        runAsyncDNSQuery(server, "frostnerd.com", false, Record.TYPE.A, Record.CLASS.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(List<Record<? extends Data>> response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void runAsyncDNSQuery(final IPPortPair server, final String query, final boolean tcp, final Record.TYPE type,
                                        final Record.CLASS clazz, final Util.DNSQueryResultListener resultListener, final int timeout){
        if(server == null)return;
        new Thread(){
            @Override
            public void run() {
                try {
                    Resolver resolver = new Resolver(server.getAddress());
                    ResolverResult<Data> result = resolver.resolve(query.endsWith(".") ? query : query + ".", type, clazz,  tcp, server.getPort());
                    if(!result.wasSuccessful()) resultListener.onError(new IllegalStateException("The query wasn't successful"));
                    if(!result.isAuthenticData()) resultListener.onError(new IllegalStateException("DNSSEC validation failed"));
                    resultListener.onSuccess(result.getDnsMessage().answerSection);
                } catch (IOException | IllegalStateException e) {
                    resultListener.onError(e);
                }
            }
        }.start();
    }

    public static List<Record<? extends Data>> runSyncDNSQuery(final IPPortPair server, final String query, final boolean tcp, Record.TYPE type,
                                                               Record.CLASS clazz, final int timeout){
        if(server == null) return null;
        try {
            Resolver resolver = new Resolver(server.getAddress());
            ResolverResult<Data> result = resolver.resolve(query.endsWith(".") ? query : query + ".", type, clazz,  tcp, server.getPort());
            if(!result.wasSuccessful()) return new ArrayList<>();
            if(!result.isAuthenticData()) return new ArrayList<>();
            return result.getDnsMessage().answerSection;
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }
}
