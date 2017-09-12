package com.frostnerd.dnschanger.services;

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

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
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
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class ConnectivityJob extends JobService{
    private BroadcastReceiver connectivityChange;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private static final String LOG_TAG = "[ConnectivityJob]";

    @Override
    public boolean onStartJob(JobParameters job) {
        LogFactory.writeMessage(this, LOG_TAG, "Job created");
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
            registerReceiver(connectivityChange = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleConnectivityChange(!intent.hasExtra("noConnectivity"), intent.getIntExtra("networkType", -1));
                }
            }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            LogFactory.writeMessage(this, LOG_TAG, "No active network.");
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Thread running: " + API.isServiceThreadRunning());
            if (!API.isServiceThreadRunning()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(this, "setting_auto_wifi", false)) {
                    LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                    startService();
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(this, "setting_auto_mobile", false)) {
                    LogFactory.writeMessage(this, LOG_TAG, "[OnCreate] Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                    startService();
                }
            }
        }
        LogFactory.writeMessage(this, LOG_TAG, "Done with onStartJob");
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters job) {
        if(networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)connectivityManager.unregisterNetworkCallback(networkCallback);
        else if(connectivityChange != null)unregisterReceiver(connectivityChange);
        return true;
    }

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
        if(Preferences.getBoolean(this, "everything_disabled", false))return;
        boolean serviceRunning = API.isServiceRunning(this),
                serviceThreadRunning = API.isServiceThreadRunning(),
                autoWifi = Preferences.getBoolean(this, "setting_auto_wifi", false),
                autoMobile = Preferences.getBoolean(this, "setting_auto_mobile", false),
                disableNetChange = Preferences.getBoolean(this, "setting_disable_netchange", false);
        LogFactory.writeMessage(this, LOG_TAG, "Connectivity changed. Connected: " + connected + ", type: " + connectionType);
        LogFactory.writeMessage(this, LOG_TAG, "Service running: " + serviceRunning + "; Thread running: " + serviceThreadRunning);
        API.updateTiles(this);
        WidgetUtil.updateAllWidgets(this, BasicWidget.class);
        Intent i;
        if(connectionType == ConnectionType.VPN)return;
        if (!connected && disableNetChange && serviceRunning) {
            LogFactory.writeMessage(this, LOG_TAG,
                    "Destroying DNSVPNService, as device is not connected and setting_disable_netchange is true",
                    i = DNSVpnService.getDestroyIntent(this, getString(R.string.reason_stop_network_change)));
            startService(i);
            return;
        }
        //if (!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)
        if(!connected || (connectionType != ConnectionType.WIFI && connectionType != ConnectionType.MOBILE))
            return;
        if (!serviceThreadRunning) {
            if (connectionType == ConnectionType.WIFI && autoWifi) {
                LogFactory.writeMessage(this, LOG_TAG, "Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            } else if (connectionType == ConnectionType.MOBILE && autoMobile) {
                LogFactory.writeMessage(this, LOG_TAG, "Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }
        }else {
            if (!(connectionType == ConnectionType.WIFI && autoWifi) && !(connectionType == ConnectionType.MOBILE && autoMobile) && Preferences.getBoolean(this, "setting_disable_netchange", false) && serviceRunning) {
                LogFactory.writeMessage(this, LOG_TAG,
                        "Not on WIFI or MOBILE and setting_disable_netchange is true. Destroying DNSVPNService.",
                        i =DNSVpnService.getDestroyIntent(this, getString(R.string.reason_stop_network_change)));
                startService(i);
            }
        }
    }

    private enum ConnectionType{
        MOBILE,WIFI,VPN,OTHER;
    }
}
