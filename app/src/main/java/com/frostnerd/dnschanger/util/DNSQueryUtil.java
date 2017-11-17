package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
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
        runAsyncDNSQuery(PreferencesAccessor.isIPv4Enabled(context) ? PreferencesAccessor.Type.DNS1.getPair(context).getAddress() :
                PreferencesAccessor.Type.DNS1_V6.getPair(context).getAddress(), "frostnerd.com", false, Type.A, DClass.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(Message response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void startDNSServerConnectivityCheck(@NonNull final Context context, @NonNull final String dnsAddress, @NonNull final Util.ConnectivityCheckCallback callback){
        runAsyncDNSQuery(dnsAddress, "frostnerd.com", false, Type.A, DClass.ANY, new Util.DNSQueryResultListener() {
            @Override
            public void onSuccess(Message response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void runAsyncDNSQuery(final String server, final String query, final boolean tcp, final int type,
                                        final int dClass, final Util.DNSQueryResultListener resultListener, final int timeout){
        new Thread(){
            @Override
            public void run() {
                try {
                    Resolver resolver = new SimpleResolver(server);
                    resolver.setTCP(tcp);
                    resolver.setTimeout(timeout);
                    Name name = Name.fromString(query.endsWith(".") ? query : query + ".");
                    Record record = Record.newRecord(name, type, dClass);
                    Message query = Message.newQuery(record);
                    Message response = resolver.send(query);
                    if(response.getSectionRRsets(1) == null)throw new IllegalStateException("Answer is null");
                    resultListener.onSuccess(response);
                } catch (IOException e) {
                    resultListener.onError(e);
                }
            }
        }.start();
    }

    public static Message runSyncDNSQuery(final String server, final String query, final boolean tcp, final int type,
                                          final int dClass, Util.DNSQueryResultListener dnsQueryResultListener, final int timeout){
        try {
            Resolver resolver = new SimpleResolver(server);
            resolver.setTCP(tcp);
            resolver.setTimeout(timeout);
            Name name = Name.fromString(query.endsWith(".") ? query : query + ".");
            Record record = Record.newRecord(name, type, dClass);
            Message mquery = Message.newQuery(record);
            Message response = resolver.send(mquery);
            if(response.getSectionRRsets(1) == null)throw new IllegalStateException("Answer is null");
            return response;
        } catch (IOException e) {
            return null;
        }
    }
}
