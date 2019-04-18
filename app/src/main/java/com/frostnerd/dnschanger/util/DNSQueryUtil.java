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
        if(server == null)return;
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
        if(server == null)return;
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
        if(server == null) return null;
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
