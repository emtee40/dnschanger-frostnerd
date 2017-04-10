package com.frostnerd.dnschanger.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class ConnectivityBackgroundService extends Service {
    private static final String LOG_TAG = "[ConnectivityBackgroundService]";
    private BroadcastReceiver connectivityChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = !intent.hasExtra("noConnectivity");
            int type = intent.getIntExtra("networkType", -1);
            LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connectivity changed. Connected: " + connected + ", type: " + type);
            DNSVpnService.updateTiles(context);
            if(!connected && Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false)){
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Destroying DNSVPNService, as device is not connected and setting_disable_netchange is true");
                startService(new Intent(ConnectivityBackgroundService.this, DNSVpnService.class).putExtra("destroy",true));
            }
            if(!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)return;
            if(type == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_wifi",false)){
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            }else if(type == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_mobile",false)){
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }else if(Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false)){
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Not on WIFI or MOBILE and setting_disable_netchange is true. Destroying DNSVPNService.");
                startService(new Intent(ConnectivityBackgroundService.this, DNSVpnService.class).putExtra("destroy",true));
            }
        }
    };

    private void startService(){
        LogFactory.writeMessage(this, LOG_TAG, "Trying to start DNSVPNService");
        Intent i = VpnService.prepare(this);
        if(i == null){
            LogFactory.writeMessage(this, LOG_TAG, "VPNService is already prepared. Starting...");
            this.startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn",true));
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "VPNService is NOT prepared. Starting BackgroundVpnConfigureActivity");
            BackgroundVpnConfigureActivity.startBackgroundConfigure(this,true);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Service was created");
        registerReceiver(connectivityChange, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null){
            LogFactory.writeMessage(this, LOG_TAG, "No active network.");
            return;
        }
        if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_wifi",false)){
            LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to WIFI and setting_auto_wifi is true. Starting Service..");
            startService();
        }else if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_mobile",false)){
            LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
            startService();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
