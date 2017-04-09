package com.frostnerd.dnschanger;

import android.app.Activity;
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
    private BroadcastReceiver connectivityChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = !intent.hasExtra("noConnectivity");
            int type = intent.getIntExtra("networkType", -1);
            DNSVpnService.updateTiles(context);
            if(!connected && Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false)) startService(new Intent(ConnectivityBackgroundService.this, DNSVpnService.class).putExtra("destroy",true));
            if(!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)return;
            if(type == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_wifi",false)){
                startService();
            }else if(type == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_mobile",false)){
                startService();
            }else if(Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false)){
                startService(new Intent(ConnectivityBackgroundService.this, DNSVpnService.class).putExtra("destroy",true));
            }
        }
    };

    private void startService(){
        Intent i = VpnService.prepare(this);
        if(i == null){
            this.startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn",true));
        }
        else BackgroundVpnConfigureActivity.startBackgroundConfigure(this,true);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(connectivityChange, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null)return;
        if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_wifi",false)){
            startService();
        }else if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(ConnectivityBackgroundService.this,"setting_auto_mobile",false)){
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
