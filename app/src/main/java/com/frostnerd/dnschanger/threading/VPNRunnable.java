package com.frostnerd.dnschanger.threading;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.system.OsConstants;

import com.frostnerd.dnschanger.DNSChanger;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.activities.InvalidDNSDialogActivity;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.dnsproxy.DNSProxy;
import com.frostnerd.dnschanger.util.dnsproxy.DummyProxy;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class VPNRunnable implements Runnable {
    private static final String LOG_TAG = "[DNSVpnService-Runnable]";
    private static final Map<String, Integer> addresses = new ConcurrentHashMap<>();
    private final String ID = "[" + StringUtil.randomString(20) + "]";
    private int addressIndex = 0;
    private ParcelFileDescriptor tunnelInterface = null;
    private VpnService.Builder builder;
    private DNSVpnService service;
    private Set<String> vpnApps;
    private List<IPPortPair> upstreamServers;
    private boolean whitelistMode, running = true;
    private final List<Runnable> afterThreadStop = new ArrayList<>();
    private DNSProxy dnsProxy;
    static{
        addresses.put("172.31.255.253", 30);
        addresses.put("192.168.0.131", 24);
        addresses.put("192.168.234.55", 24);
        addresses.put("172.31.255.1", 28);
    }

    public VPNRunnable(@NonNull DNSVpnService service, @NonNull List<IPPortPair> upstreamServers, @NonNull Set<String> vpnApps, boolean whitelistMode){
        if(service == null)throw new IllegalStateException("The DNSVPNService passed to VPNRunnable is null.");
        this.service = service;
        this.whitelistMode = whitelistMode;
        this.vpnApps = vpnApps;
        this.upstreamServers = new ArrayList<>(upstreamServers.size());
        for(IPPortPair pair: upstreamServers){ //Remap the loopback addresses to its replacements
            IPPortPair newPair = pair;
            if(pair.getAddress().equals("127.0.0.1")) {
                newPair = new IPPortPair(pair);
                newPair.setIp(DNSProxy.IPV4_LOOPBACK_REPLACEMENT);
            }else if(pair.getAddress().equals("::1")){
                newPair = new IPPortPair(pair);
                newPair.setIp(DNSProxy.IPV6_LOOPBACK_REPLACEMENT);
            }
            this.upstreamServers.add(newPair);
        }
    }

    @Override
    public void run() {
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Starting Thread (run)");
        Thread.setDefaultUncaughtExceptionHandler(((DNSChanger)service.getApplicationContext()).getExceptionHandler());
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Trying " + addresses.size() + " different addresses before passing any thrown exception to the upper layer");
        try{
            for(String address: addresses.keySet()){
                if(!running)break;
                addressIndex++;
                try{
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Trying address '" + address + "'");
                    configure(address, PreferencesAccessor.isRunningInAdvancedMode(service));
                    tunnelInterface = builder.establish();
                    if(tunnelInterface == null){
                        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface is null, service is not prepared.");
                        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Starting Background");
                        BackgroundVpnConfigureActivity.startBackgroundConfigure(service, true);
                        break;
                    }
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface connected.");
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcasting current state");
                    service.broadcastCurrentState();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcast sent");
                    service.updateNotification();
                    Util.updateAppShortcuts(service);
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread going into while loop");
                    if(PreferencesAccessor.isRunningInAdvancedMode(service) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
                        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "We are in advanced mode, starting DNS proxy");
                        dnsProxy = DNSProxy.createProxy(service, tunnelInterface, new HashSet<>(upstreamServers)
                                ,PreferencesAccessor.areRulesEnabled(service), PreferencesAccessor.isQueryLoggingEnabled(service));
                        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "DNS proxy created");
                    }else dnsProxy = new DummyProxy();
                    dnsProxy.run();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread reached end of while loop.");
                }catch(Exception e){
                    if(!running)break;
                    LogFactory.writeStackTrace(service, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]", ID}, e);
                    if(addressIndex >= addresses.size())throw e;
                    else LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]", ID},
                            "Not throwing exception. Tries: " + addressIndex + ", addresses: " + addresses.size());
                }
            }
        }catch(Exception e){
            LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread had an exception");
            LogFactory.writeStackTrace(service, new String[]{LOG_TAG,LogFactory.Tag.ERROR.toString()}, e);
            if(isDNSInvalid(e))service.startActivity(new Intent(service, InvalidDNSDialogActivity.class));
            else Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
        }finally {
            running = false;
            LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread is in finally block");
            Util.updateAppShortcuts(service);
            Util.updateTiles(service);
            service.updateNotification();
            service.broadcastCurrentState();
            synchronized (afterThreadStop){
                for(Runnable r: afterThreadStop)r.run();
                afterThreadStop.clear();
            }
            cleanup();
            LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Done with finally block");
        }
    }

    public void addAfterThreadStop(@NonNull Runnable runnable){
        afterThreadStop.add(runnable);
    }

    private boolean isDNSInvalid(@NonNull Exception ex){
        for(StackTraceElement ste: ex.getStackTrace())
            if(ste.toString().contains("Builder.addDnsServer") && ex instanceof IllegalArgumentException && ex.getMessage().contains("Bad address"))return true;
        return false;
    }

    private void cleanup(){
        running = false;
        if(tunnelInterface != null){
            try {
                tunnelInterface.close();
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        builder = null;
        tunnelInterface = null;
        dnsProxy = null;
        builder = null;
    }

    private void configure(@NonNull String address, boolean advanced){
        boolean ipv6Enabled = PreferencesAccessor.isIPv6Enabled(service), ipv4Enabled = PreferencesAccessor.isIPv4Enabled(service);
        if(!ipv4Enabled && !ipv6Enabled)ipv4Enabled = true;
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Creating Tunnel interface");
        builder = service.createBuilder();
        builder.setSession("dnsChanger_frostnerd");
        if (advanced && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setBlocking(true);
        }
        if(ipv4Enabled){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(OsConstants.AF_INET);
            }
            builder = builder.addAddress(address, addresses.get(address));
        }
        if(ipv6Enabled){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(OsConstants.AF_INET6);
            }
            builder = builder.addAddress(NetworkUtil.randomLocalIPv6Address(),48);
        }
        for(IPPortPair pair: upstreamServers){
            if((pair.isIpv6() && ipv6Enabled) || (!pair.isIpv6() && ipv4Enabled))
                addDNSServer(pair.getAddress(), advanced, pair.isIpv6());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try{
                if(whitelistMode){
                    for(String s: vpnApps){
                        if(s.equals("com.android.vending"))continue;
                        builder = builder.addAllowedApplication(s);
                    }
                }else{
                    builder = builder.addDisallowedApplication("com.android.vending");
                    for(String s: vpnApps){
                        if(s.equals("com.android.vending"))continue;
                        builder = builder.addDisallowedApplication(s);
                    }
                }
            }catch (PackageManager.NameNotFoundException ignored){

            }
        }
        String release = Build.VERSION.RELEASE;
        if(Build.VERSION.SDK_INT != 19 || release.startsWith("4.4.3") || release.startsWith("4.4.4") || release.startsWith("4.4.5") || release.startsWith("4.4.6")){
            builder.setMtu(1500);
        }else builder.setMtu(1280);
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface created, not yet connected");
        builder.setConfigureIntent(PendingIntent.getActivity(service, 1, new Intent(service, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT));
    }
    public static final Map<String, InetAddress> addressRemap = new HashMap<>();
    private final String addressRemapBase = "244.0.0.";
    private int addressRemapIndex = 1;

    private void addDNSServer(@NonNull String server, boolean addRoute, boolean ipv6){
        if(server != null && !server.equals("")){
            if(server.equals("127.0.0.1"))server = DNSProxy.IPV4_LOOPBACK_REPLACEMENT;
            else if(server.equals("::1"))server = DNSProxy.IPV6_LOOPBACK_REPLACEMENT;
            else if(PreferencesAccessor.sendDNSOverTLS(service)) {
                addressRemap.put(addressRemapBase + addressRemapIndex++, getRemappedAddress(server));
                server = addressRemapBase + (addressRemapIndex-1);
            }
            builder.addDnsServer(server);
            if(addRoute) builder.addRoute(server, ipv6 ? 128 : 32);
        }
    }

    private InetAddress getRemappedAddress(String server){
        try {
            return InetAddress.getByName(server);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isThreadRunning(){
        return running;
    }

    public void stop(@Nullable Thread thread){
        running = false;
        if(dnsProxy != null) dnsProxy.stop();
        if(thread != null) thread.interrupt();
    }

    public void destroy(){
        running = false;
        cleanup();
        vpnApps.clear();
        upstreamServers.clear();
        vpnApps = null;
        service = null;
    }
}
