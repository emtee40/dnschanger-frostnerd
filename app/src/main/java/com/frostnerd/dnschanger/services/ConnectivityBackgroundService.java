package com.frostnerd.dnschanger.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.widgets.BasicWidget;
import com.frostnerd.utils.general.WidgetUtil;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class ConnectivityBackgroundService extends Service {
    private static final String LOG_TAG = "[ConnectivityBackgroundService]";
    ConnectivityManager connectivityManager;
    private BroadcastReceiver connectivityChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleConnectivityChange(!intent.hasExtra("noConnectivity"), intent.getIntExtra("networkType", -1));
        }
    };
    private ConnectivityManager.NetworkCallback networkCallback;

    private void startService() {
        LogFactory.writeMessage(this, LOG_TAG, "Trying to start DNSVPNService");
        Intent i = VpnService.prepare(this);
        LogFactory.writeMessage(this, LOG_TAG, "VPNService Prepare Intent", i);
        if (i == null) {
            LogFactory.writeMessage(this, LOG_TAG, "VPNService is already prepared. Starting DNSVPNService",
                    i = DNSVpnService.getStartVPNIntent(this).putExtra(VPNServiceArgument.FLAG_DONT_START_IF_RUNNING.getArgument(), true).
                            putExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(),false));
            API.startService(this, i);
        } else {
            LogFactory.writeMessage(this, LOG_TAG, "VPNService is NOT prepared. Starting BackgroundVpnConfigureActivity");
            BackgroundVpnConfigureActivity.startBackgroundConfigure(this, true);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Service was created");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(Network network) {
                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                    handleConnectivityChange(activeNetwork);
                }

                @Override
                public void onLost(Network network) {
                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                    handleConnectivityChange(activeNetwork);
                }
            });
        }else{
            registerReceiver(connectivityChange, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            LogFactory.writeMessage(this, LOG_TAG, "No active network.");
            return;
        }
        LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Thread running: " + API.isServiceThreadRunning());
        if (!API.isServiceThreadRunning()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_auto_wifi", false)) {
                LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_auto_mobile", false)) {
                LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogFactory.writeMessage(this, LOG_TAG, "StartCommand received", intent);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogFactory.writeMessage(this, LOG_TAG, "Bind command received", intent);
        return null;
    }

    @Override
    public void onDestroy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)unregisterReceiver(connectivityChange);
        else connectivityManager.unregisterNetworkCallback(networkCallback);
        super.onDestroy();
    }

    private void handleConnectivityChange(NetworkInfo networkInfo){
        if(networkInfo != null)handleConnectivityChange(networkInfo.isConnected(), networkInfo.getType());
        else handleConnectivityChange(false, ConnectionType.OTHER);
    }

    private void handleConnectivityChange(boolean connected, int type){
        ConnectionType cType;
        if(type == ConnectivityManager.TYPE_WIFI)cType = ConnectionType.WIFI;
        else if(type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_MOBILE_DUN)cType = ConnectionType.MOBILE;
        else if(type == ConnectivityManager.TYPE_VPN)cType = ConnectionType.VPN;
        else cType = ConnectionType.OTHER;
        handleConnectivityChange(connected, cType);
    }

    private void handleConnectivityChange(boolean connected, ConnectionType connectionType){
        boolean serviceRunning = API.isServiceRunning(ConnectivityBackgroundService.this),
                serviceThreadRunning = API.isServiceThreadRunning(),
                autoWifi = Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_auto_wifi", false),
                autoMobile = Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_auto_mobile", false),
                disableNetChange = Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false);
        LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connectivity changed. Connected: " + connected + ", type: " + connectionType);
        LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Service running: " + serviceRunning + "; Thread running: " + serviceThreadRunning);
        API.updateTiles(this);
        WidgetUtil.updateAllWidgets(this, BasicWidget.class);
        Intent i;
        if(connectionType == ConnectionType.VPN)return;
        if (!connected && disableNetChange && serviceRunning) {
            LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG,
                    "Destroying DNSVPNService, as device is not connected and setting_disable_netchange is true",
                    i = DNSVpnService.getDestroyIntent(ConnectivityBackgroundService.this, getString(R.string.reason_stop_network_change)));
            startService(i);
            return;
        }
        //if (!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)
        if(!connected || (connectionType != ConnectionType.WIFI && connectionType != ConnectionType.MOBILE))
            return;
        if (!serviceThreadRunning) {
            if (connectionType == ConnectionType.WIFI && autoWifi) {
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            } else if (connectionType == ConnectionType.MOBILE && autoMobile) {
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG, "Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }
        }else {
            if (!(connectionType == ConnectionType.WIFI && autoWifi) && !(connectionType == ConnectionType.MOBILE && autoMobile) && Preferences.getBoolean(ConnectivityBackgroundService.this, "setting_disable_netchange", false) && serviceRunning) {
                LogFactory.writeMessage(ConnectivityBackgroundService.this, LOG_TAG,
                        "Not on WIFI or MOBILE and setting_disable_netchange is true. Destroying DNSVPNService.",
                        i =DNSVpnService.getDestroyIntent(ConnectivityBackgroundService.this, getString(R.string.reason_stop_network_change)));
                startService(i);
            }
        }
    }

    private enum ConnectionType{
        MOBILE,WIFI,VPN,OTHER;
    }
}
