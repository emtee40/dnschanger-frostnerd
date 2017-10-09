package com.frostnerd.dnschanger.threading;

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
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.IOException;
import java.util.ArrayList;
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
    private boolean whitelistMode, fixedDNS, startedWithTasker, running = true;
    private final List<Runnable> afterThreadStop = new ArrayList<>();
    static{
        addresses.put("172.31.255.253", 30);
        addresses.put("192.168.0.131", 24);
        addresses.put("192.168.234.55", 24);
        addresses.put("172.31.255.1", 28);
    }

    public VPNRunnable(DNSVpnService service, String dns1, String dns2, String dns1v6, String dns2v6, Set<String> vpnApps, boolean whitelistMode, boolean fixedDNS, boolean startedWithTasker){
        this.service = service;
        this.dns1 = dns1;
        this.dns1v6 = dns1v6;
        this.dns2 = dns2;
        this.dns2v6 = dns2v6;
        this.whitelistMode = whitelistMode;
        this.vpnApps = vpnApps;
        this.startedWithTasker = startedWithTasker;
        this.fixedDNS = fixedDNS;
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
                    configure(address);
                    tunnelInterface = builder.establish();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface connected.");
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcasting current state");
                    service.broadcastCurrentState();
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcast sent");
                    service.updateNotification();
                    API.updateAppShortcuts(service);
                    LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread going into while loop");
                    while(running){
                        Thread.sleep(250);
                    }
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

    private void configure(String address){
        boolean ipv6Enabled = API.isIPv6Enabled(service), ipv4Enabled = API.isIPv4Enabled(service);
        LogFactory.writeMessage(service, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Creating Tunnel interface");
        builder = service.createBuilder();
        builder.setSession("DnsChanger");
        if(ipv4Enabled){
            builder = builder.addAddress(address, addresses.get(address));
            addDNSServer(dns1);
            addDNSServer(dns2);
        }
        if(ipv6Enabled){
            builder = builder.addAddress(NetworkUtil.randomLocalIPv6Address(),48);
            addDNSServer(dns1v6);
            addDNSServer(dns2v6);
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

    private void addDNSServer(String server){
        if(server != null && !server.equals(""))builder.addDnsServer(server);
    }

    public boolean isThreadRunning(){
        return running;
    }

    public void stop(Thread thread){
        running = false;
        thread.interrupt();
    }

    public void destroy(){
        running = false;
        cleanup();
        vpnApps.clear();
        vpnApps = null;
        service = null;
    }
}
