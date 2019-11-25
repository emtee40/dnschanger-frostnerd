package com.frostnerd.dnschangertests;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Base64;

import com.frostnerd.dnstunnelproxy.DnsPacketProxy;
import com.frostnerd.dnstunnelproxy.UpstreamAddress;
import com.frostnerd.encrypteddnstunnelproxy.AbstractHttpsDNSHandle;
import com.frostnerd.encrypteddnstunnelproxy.Scheduler;
import com.frostnerd.encrypteddnstunnelproxy.ServerConfiguration;
import com.frostnerd.encrypteddnstunnelproxy.UrlCreator;
import com.frostnerd.networking.NetworkUtil;
import com.frostnerd.vpntunnelproxy.PacketProxy;
import com.frostnerd.vpntunnelproxy.ReceivedAnswer;
import com.frostnerd.vpntunnelproxy.VPNTunnelProxy;

import org.jetbrains.annotations.NotNull;
import org.minidns.dnsmessage.DnsMessage;
import org.pcap4j.packet.IpPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

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
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TestVPNService extends VpnService implements Runnable {
    private ParcelFileDescriptor fd;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Builder builder = new Builder();
        builder.addAddress("192.168.0.10", 24);
        builder.addAddress(NetworkUtil.randomLocalIPv6Address(), 48);
        builder.addDnsServer("8.8.8.8");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setBlocking(true);
        }
        builder.addRoute("8.8.8.8", 32);
        builder.setSession("DNS Test");
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.allowFamily(OsConstants.AF_INET);
            builder.allowFamily(OsConstants.AF_INET6);
        }
        fd = builder.establish();
        System.out.println(">>>>>>>>>>>>>>>>");
        System.out.println("Established");
        new Thread(this).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        System.out.println("Run...");
        https();
    }

    private void normal() {
        PacketProxy packetProxy = new PacketProxy(this) {
            @Override
            public void processUpstreamResponse(@NotNull ReceivedAnswer receivedAnswer) {

            }

            @Override
            public void processDevicePacket(@NotNull byte[] bytes) {

            }
        };
        VPNTunnelProxy proxy = new VPNTunnelProxy(packetProxy);
        try {
            proxy.run(fd);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
    }

    private void https() {
        final ServerConfiguration serverConfiguration = new ServerConfiguration(new UrlCreator() {
            @NotNull
            @Override
            public URL createUrl(@NotNull DnsMessage dnsMessage, @NotNull UpstreamAddress upstreamAddress) {
                String encoded = Base64.encodeToString(dnsMessage.toArray(), Base64.DEFAULT);
                try {
                    URL url = new URL("https://cloudflare-dns.com/dns-query?dns=" + encoded);
                    System.out.println(url);
                    return url;
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }, false, null);


        final VPNTunnelProxy proxy = new VPNTunnelProxy(new DnsPacketProxy(new AbstractHttpsDNSHandle(serverConfiguration, 5000, new Scheduler() {
            @Override
            public void schedule(@NotNull final Function0<Unit> function0) {
                new Thread() {
                    @Override
                    public void run() {
                        function0.invoke();
                    }
                }.start();
            }
        }) {
            @Override
            public boolean shouldHandleDestination(@NotNull InetAddress inetAddress, int i) {
                System.out.println("SHould handle: " + inetAddress);
                return true;
            }

            @NotNull
            @Override
            public UpstreamAddress remapDestination(@NotNull InetAddress inetAddress, int i) {
                System.out.println("Remapping " + inetAddress + ":" + i);
                return new UpstreamAddress(inetAddress, i);
            }

            @NotNull
            @Override
            public DnsMessage modifyUpstreamResponse(@NotNull DnsMessage dnsMessage) {
                return null;
            }

            @Override
            public boolean shouldModifyUpstreamResponse(@NotNull ReceivedAnswer receivedAnswer, @NotNull byte[] bytes) {
                return false;
            }

            @Override
            public void forwardDnsQuestion(@NotNull DnsMessage dnsMessage, @NotNull IpPacket originalEnvelope, @NotNull UpstreamAddress realDestination) {
                System.out.println("Forwarding " + dnsMessage);
                System.out.println("Stats: " + getDnsPacketProxy().getTunnelHandle().getTrafficStats());
                super.forwardDnsQuestion(dnsMessage, originalEnvelope, realDestination);
            }
        }, this, null, null));

        try {
            System.out.println("Now running proxy");
            proxy.run(fd);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
    }
}
