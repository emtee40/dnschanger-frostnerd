package com.frostnerd.dnschanger;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import com.frostnerd.utils.preferences.Preferences;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DNSVpnService extends VpnService {
    private boolean run = true, isRunning = false;
    private Thread thread;
    private ParcelFileDescriptor tunnelInterface;
    private Builder builder = new Builder();

    @Override
    public void onDestroy() {
        run = false;
        if(thread != null)thread.interrupt();
        thread = null;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("stop_vpn", false)) {
            if (thread != null) {
                run = false;
                thread.interrupt();
                thread = null;
            }
        } else if(intent.getBooleanExtra("start_vpn", false)){
            if (thread != null) {
                run = false;
                thread.interrupt();
            }
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String dns1 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.8.8"),
                                dns2 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.4.4");
                        tunnelInterface = builder.setSession("DnsChanger").addAddress("192.168.0.1", 24).addDnsServer(dns1).addDnsServer(dns2).establish();
                        DatagramChannel tunnel = DatagramChannel.open();
                        tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                        protect(tunnel.socket());
                        isRunning = true;
                        try {
                            while(run){
                                Thread.sleep(100);
                            }
                        }catch(InterruptedException e2){

                        }
                        isRunning = false;
                    } catch (IOException e) {

                    }finally {
                        if(tunnelInterface != null) try {
                            tunnelInterface.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            run = true;
            thread.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
