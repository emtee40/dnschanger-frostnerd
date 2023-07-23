package com.frostnerd.dnschanger.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.dnsquery.Resolver;
import com.frostnerd.dnschanger.util.dnsquery.ResolverResult;

import org.minidns.record.Data;
import org.minidns.record.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
                PreferencesAccessor.Type.DNS1_V6.getPair(context), "frostnerd.com", false, Record.TYPE.A, Record.CLASS.IN, new Util.DNSQueryResultListener() {
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
        runAsyncDNSQuery(server, "frostnerd.com", false, Record.TYPE.A, Record.CLASS.IN, new Util.DNSQueryResultListener() {
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
                    ResolverResult<Data> result = resolver.resolve(query, type, clazz,  tcp, server.getPort());
                    if(!result.wasSuccessful()) resultListener.onError(new IllegalStateException("The query wasn't successful"));
                    resultListener.onSuccess(result.getRawAnswer().answerSection);
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
            ResolverResult<Data> result = resolver.resolve(query, type, clazz,  tcp, server.getPort());
            if(!result.wasSuccessful()) return new ArrayList<>();
            return result.getRawAnswer().answerSection;
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }
}
