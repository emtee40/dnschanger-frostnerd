package com.frostnerd.dnschanger.threading;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import com.frostnerd.dnschanger.DNSChanger;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.InvalidDNSDialogActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.API;
import com.frostnerd.dnschanger.util.dnsproxy.DNSProxy;
import com.frostnerd.dnschanger.util.dnsproxy.DNSUDPProxy;
import com.frostnerd.dnschanger.util.dnsproxy.DummyProxy;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.preferences.Preferences;

import java.io.IOException;
import java.util.ArrayList;
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
    private static final Map<String, Integer> addresses = new ConcurrentHashMap<String, Integer>();
    private final String ID = "[" + StringUtil.randomString(20) + "]";
    private int addressIndex = 0;
    private ParcelFileDescriptor tunnelInterface = null;
    private VpnService.Builder builder;
    private DNSVpnService service;
    private String dns1, dns2, dns1v6, dns2v6;
    private Set<String> vpnApps;
    private boolean whitelistMode, running = true;
    private final List<Runnable> afterThreadStop = new ArrayList<>();
    private DNSProxy dnsProxy;
    static{
        addresses.put("172.31.255.253", 30);
        addresses.put("192.168.0.131", 24);
        addresses.put("192.168.234.55", 24);
        addresses.put("172.31.255.1", 28);
    }

    public VPNRunnable(DNSVpnService service, String dns1, String dns2, String dns1v6, String dns2v6, Set<String> vpnApps, boolean whitelistMode){
        this.service = service;
        this.dns1 = dns1;
        this.dns1v6 = dns1v6;
        this.dns2 = dns2;
        this.dns2v6 = dns2v6;
        this.whitelistMode = whitelistMode;
        this.vpnApps = vpnApps;
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
                    configure(address, isInAdvancedMode(service));
                    tunnelInterface = builder.establish();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface connected.");
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcasting current state");
                    service.broadcastCurrentState();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcast sent");
                    service.updateNotification();
                    API.updateAppShortcuts(service);
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread going into while loop");
                    if(isInAdvancedMode(service) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
                        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "We are in advanced mode, starting DNS proxy");
                        dnsProxy = new DNSUDPProxy(service, tunnelInterface, new HashSet<>(API.getAllDNSPairs(service))
                                ,Preferences.getBoolean(service, "rules_activated", false), Preferences.getBoolean(service, "query_logging", false));
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
                }finally {
                    cleanup();
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
            API.updateAppShortcuts(service);
            API.updateTiles(service);
            cleanup();
            service.updateNotification();
            service.broadcastCurrentState();
            synchronized (afterThreadStop){
                for(Runnable r: afterThreadStop)r.run();
                afterThreadStop.clear();
            }
            LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Done with finally block");
        }
    }

    public void addAfterThreadStop(Runnable runnable){
        afterThreadStop.add(runnable);
    }

    private boolean isDNSInvalid(Exception ex){
        for(StackTraceElement ste: ex.getStackTrace())
            if(ste.toString().contains("Builder.addDnsServer") && ex instanceof IllegalArgumentException && ex.getMessage().contains("Bad address"))return true;
        return false;
    }

    private void cleanup(){
        if(tunnelInterface != null){
            try {
                tunnelInterface.close();
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
        }
        builder = null;
        tunnelInterface = null;
    }

    private void configure(String address, boolean advanced){
        boolean ipv6Enabled = API.isIPv6Enabled(service), ipv4Enabled = API.isIPv4Enabled(service);
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Creating Tunnel interface");
        builder = service.createBuilder();
        builder.setSession("DnsChanger");
        if(ipv4Enabled){
            builder = builder.addAddress(address, addresses.get(address));
            addDNSServer(dns1, advanced);
            addDNSServer(dns2, advanced);
        }
        if(ipv6Enabled){
            builder = builder.addAddress(NetworkUtil.randomLocalIPv6Address(),48);
            addDNSServer(dns1v6, advanced);
            addDNSServer(dns2v6, advanced);
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
    }

    private void addDNSServer(String server, boolean addRoute){
        if(server != null && !server.equals("")){
            builder.addDnsServer(server);
            if(addRoute)builder.addRoute(server, 32);
        }
    }

    public static boolean isInAdvancedMode(Context context){
        return Preferences.getBoolean(context, "advanced_settings", false) &&
                (Preferences.getBoolean(context, "custom_port", false) ||
                        Preferences.getBoolean(context, "rules_activated", false) ||
                        Preferences.getBoolean(context, "query_logging", false));
    }

    public boolean isThreadRunning(){
        return running;
    }

    public void stop(Thread thread){
        running = false;
        if(dnsProxy != null)dnsProxy.stop();
        cleanup();
        thread.interrupt();
    }

    public void destroy(){
        running = false;
        cleanup();
        vpnApps.clear();
        vpnApps = null;
    }
}
