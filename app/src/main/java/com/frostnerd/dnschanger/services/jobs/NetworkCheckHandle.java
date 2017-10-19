package com.frostnerd.dnschanger.services.jobs;

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

import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
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
public class NetworkCheckHandle {
    private BroadcastReceiver connectivityChange;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private Context context;
    private final String LOG_TAG;
    private boolean running = true;

    public NetworkCheckHandle(Context context, String logTag){
        this.context = context;
        LOG_TAG = logTag;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback = new ConnectivityManager.NetworkCallback(){
                @Override
                public void onAvailable(Network network) {
                    if(running){
                        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                        handleConnectivityChange(activeNetwork);
                    }
                }

                @Override
                public void onLost(Network network) {
                    if(running){
                        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                        handleConnectivityChange(activeNetwork);
                    }
                }
            });
        }else{
            context.registerReceiver(connectivityChange = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleConnectivityChange(!intent.hasExtra("noConnectivity"), intent.getIntExtra("networkType", -1));
                }
            }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        handleInitialState();
    }

    private void handleInitialState(){
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            LogFactory.writeMessage(context, LOG_TAG, "No active network.");
        }else{
            LogFactory.writeMessage(context, LOG_TAG, "[OnCreate] Thread running: " + Util.isServiceThreadRunning());
            if (!Util.isServiceThreadRunning()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getBoolean(context, "setting_auto_wifi", false)) {
                    LogFactory.writeMessage(context, LOG_TAG, "[OnCreate] Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                    startService();
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getBoolean(context, "setting_auto_mobile", false)) {
                    LogFactory.writeMessage(context, LOG_TAG, "[OnCreate] Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                    startService();
                }
            }
        }
    }

    public void stop(){
        running = false;
        if(networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)connectivityManager.unregisterNetworkCallback(networkCallback);
        else if(connectivityChange != null)context.unregisterReceiver(connectivityChange);
        connectivityManager = null;networkCallback = null;
        context = null;
    }

    private void startService() {
        if(!running || context == null)return;
        LogFactory.writeMessage(context, LOG_TAG, "Trying to start DNSVPNService");
        Intent i = VpnService.prepare(context);
        LogFactory.writeMessage(context, LOG_TAG, "VPNService Prepare Intent", i);
        if (i == null) {
            LogFactory.writeMessage(context, LOG_TAG, "VPNService is already prepared. Starting DNSVPNService",
                    i = DNSVpnService.getStartVPNIntent(context).putExtra(VPNServiceArgument.FLAG_DONT_START_IF_RUNNING.getArgument(), true).
                            putExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(),false));
            Util.startService(context, i);
        } else {
            LogFactory.writeMessage(context, LOG_TAG, "VPNService is NOT prepared. Starting BackgroundVpnConfigureActivity");
            BackgroundVpnConfigureActivity.startBackgroundConfigure(context, true);
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
        if(PreferencesAccessor.isEverythingDisabled(context) || !running)return;
        boolean serviceRunning = Util.isServiceRunning(context),
                serviceThreadRunning = Util.isServiceThreadRunning(),
                autoWifi = Preferences.getBoolean(context, "setting_auto_wifi", false),
                autoMobile = Preferences.getBoolean(context, "setting_auto_mobile", false),
                disableNetChange = Preferences.getBoolean(context, "setting_disable_netchange", false);
        LogFactory.writeMessage(context, LOG_TAG, "Connectivity changed. Connected: " + connected + ", type: " + connectionType);
        LogFactory.writeMessage(context, LOG_TAG, "Service running: " + serviceRunning + "; Thread running: " + serviceThreadRunning);
        Util.updateTiles(context);
        WidgetUtil.updateAllWidgets(context, BasicWidget.class);
        Intent i;
        if(connectionType == ConnectionType.VPN)return;
        if (!connected && disableNetChange && serviceRunning) {
            LogFactory.writeMessage(context, LOG_TAG,
                    "Destroying DNSVPNService, as device is not connected and setting_disable_netchange is true",
                    i = DNSVpnService.getDestroyIntent(context, context.getString(R.string.reason_stop_network_change)));
            context.startService(i);
            return;
        }
        //if (!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)
        if(!connected || (connectionType != ConnectionType.WIFI && connectionType != ConnectionType.MOBILE))
            return;
        if (!serviceThreadRunning) {
            if (connectionType == ConnectionType.WIFI && autoWifi) {
                LogFactory.writeMessage(context, LOG_TAG, "Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            } else if (connectionType == ConnectionType.MOBILE && autoMobile) {
                LogFactory.writeMessage(context, LOG_TAG, "Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }
        }else {
            if (!(connectionType == ConnectionType.WIFI && autoWifi) && !(connectionType == ConnectionType.MOBILE && autoMobile) && Preferences.getBoolean(context, "setting_disable_netchange", false) && serviceRunning) {
                LogFactory.writeMessage(context, LOG_TAG,
                        "Not on WIFI or MOBILE and setting_disable_netchange is true. Destroying DNSVPNService.",
                        i =DNSVpnService.getDestroyIntent(context, context.getString(R.string.reason_stop_network_change)));
                context.startService(i);
            }
        }
    }

    private enum ConnectionType{
        MOBILE,WIFI,VPN,OTHER;
    }
}
