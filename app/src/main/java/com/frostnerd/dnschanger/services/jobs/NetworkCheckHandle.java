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

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.dnschanger.widgets.BasicWidget;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.general.WidgetUtil;

import java.util.HashSet;
import java.util.Set;

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
public class NetworkCheckHandle {
    private static Set<ConnectivityManager.NetworkCallback> previousCallbacks = new HashSet<>();
    private BroadcastReceiver connectivityChange;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private Context context;
    private final String LOG_TAG;
    private boolean running = true, wasAnotherVPNRunning = false;
    private final long start; // the first network callback received is the initial state. Just ignore any change the first 250ms.

    public NetworkCheckHandle(Context context, String logTag, boolean handleInitialState){
        if(context == null)throw new IllegalStateException("Context passed to NetworkCheckHandle is null.");
        start = System.currentTimeMillis();
        this.context = context;
        LOG_TAG = logTag;
        connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (ConnectivityManager.NetworkCallback previousCallback : previousCallbacks) {
                connectivityManager.unregisterNetworkCallback(previousCallback);
            }
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            try {
                connectivityManager.registerNetworkCallback(builder.build(), networkCallback = new ConnectivityManager.NetworkCallback(){
                    @Override
                    public void onAvailable(Network network) {
                        handleChange();
                    }

                    @Override
                    public void onLost(Network network) {
                        handleChange();
                    }

                    private void handleChange(){
                        if(System.currentTimeMillis() - start < 250)return;
                        if(running){
                            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                            try {
                                handleConnectivityChange(activeNetwork);
                            } catch (ReallyWeiredExceptionOnlyAFewPeopleHave ignored) {}//Look below.
                        }
                    }
                });
                previousCallbacks.add(networkCallback);
            } catch (Exception ignored) { }
        }else{
            context.registerReceiver(connectivityChange = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        handleConnectivityChange(!intent.hasExtra("noConnectivity"), intent.getIntExtra("networkType", -1));
                    } catch (ReallyWeiredExceptionOnlyAFewPeopleHave ignored) {}//Look below.
                }
            }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        try {
            if(handleInitialState) handleInitialState();
        } catch (ReallyWeiredExceptionOnlyAFewPeopleHave ignored) {}//Look below
    }

    private void handleInitialState() throws ReallyWeiredExceptionOnlyAFewPeopleHave {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            LogFactory.writeMessage(accessContext(), LOG_TAG, "No active network.");
        }else{
            LogFactory.writeMessage(accessContext(), LOG_TAG, "[OnCreate] Thread running: " + Util.isServiceThreadRunning());
            if (!Util.isServiceThreadRunning()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && Preferences.getInstance(accessContext()).getBoolean( "setting_auto_wifi", false)) {
                    LogFactory.writeMessage(accessContext(), LOG_TAG, "[OnCreate] Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                    startService();
                } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE && Preferences.getInstance(accessContext()).getBoolean( "setting_auto_mobile", false)) {
                    LogFactory.writeMessage(accessContext(), LOG_TAG, "[OnCreate] Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                    startService();
                }
            }
        }
    }

    public void stop(){
        if(!running)return;
        running = false;
        if(networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            previousCallbacks.remove(networkCallback);
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) { }
        }
        else if(connectivityChange != null)context.unregisterReceiver(connectivityChange);
        connectivityManager = null;networkCallback = null;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        stop();
    }

    private void startService() throws ReallyWeiredExceptionOnlyAFewPeopleHave {
        if(!running)return;
        LogFactory.writeMessage(accessContext(), LOG_TAG, "Trying to start DNSVPNService");
        Intent i = VpnService.prepare(accessContext());
        LogFactory.writeMessage(accessContext(), LOG_TAG, "VPNService Prepare Intent", i);
        if (i == null) {
            LogFactory.writeMessage(accessContext(), LOG_TAG, "VPNService is already prepared. Starting DNSVPNService",
                    i = DNSVpnService.getStartVPNIntent(accessContext()).putExtra(VPNServiceArgument.FLAG_DONT_START_IF_RUNNING.getArgument(), true).
                            putExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(),false));
            Util.startService(accessContext(), i);
        } else {
            LogFactory.writeMessage(accessContext(), LOG_TAG, "VPNService is NOT prepared. Starting BackgroundVpnConfigureActivity");
            BackgroundVpnConfigureActivity.startBackgroundConfigure(accessContext(), true);
        }
    }

    private void handleConnectivityChange(NetworkInfo networkInfo) throws ReallyWeiredExceptionOnlyAFewPeopleHave {
        if(networkInfo != null)handleConnectivityChange(networkInfo.isConnected(), networkInfo.getType());
        else handleConnectivityChange(false, ConnectionType.OTHER);
    }

    private void handleConnectivityChange(boolean connected, int type) throws ReallyWeiredExceptionOnlyAFewPeopleHave {
        ConnectionType cType;
        if(type == ConnectivityManager.TYPE_WIFI)cType = ConnectionType.WIFI;
        else if(type == ConnectivityManager.TYPE_MOBILE || type == ConnectivityManager.TYPE_MOBILE_DUN)cType = ConnectionType.MOBILE;
        else if(type == ConnectivityManager.TYPE_VPN)cType = ConnectionType.VPN;
        else cType = ConnectionType.OTHER;
        handleConnectivityChange(connected, cType);
    }

    private void handleConnectivityChange(boolean connected, ConnectionType connectionType) throws ReallyWeiredExceptionOnlyAFewPeopleHave {
        if(!running || PreferencesAccessor.isEverythingDisabled(accessContext()))return;
        boolean serviceRunning = Util.isServiceRunning(accessContext()),
                serviceThreadRunning = Util.isServiceThreadRunning(),
                autoWifi = Preferences.getInstance(accessContext()).getBoolean("setting_auto_wifi", false),
                autoMobile = Preferences.getInstance(accessContext()).getBoolean("setting_auto_mobile", false),
                disableNetChange = Preferences.getInstance(accessContext()).getBoolean( "setting_disable_netchange", false);
        LogFactory.writeMessage(accessContext(), LOG_TAG, "Connectivity changed. Connected: " + connected + ", type: " + connectionType);
        LogFactory.writeMessage(accessContext(), LOG_TAG, "Service running: " + serviceRunning + "; Thread running: " + serviceThreadRunning);
        Util.updateTiles(accessContext());
        WidgetUtil.updateAllWidgets(accessContext(), BasicWidget.class);
        Intent i;
        if(connectionType == ConnectionType.VPN){
            if(!Util.isServiceThreadRunning() && connected)
                wasAnotherVPNRunning = true;
            return;
        }else {
            if(connected && wasAnotherVPNRunning && Preferences.getInstance(accessContext()).getBoolean( "start_service_when_available", false)){
                wasAnotherVPNRunning = false;
                Preferences.getInstance(accessContext()).put("start_service_when_available", false);
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context, true);
            }
        }
        if (!connected && disableNetChange && serviceRunning) {
            LogFactory.writeMessage(accessContext(), LOG_TAG,
                    "Destroying DNSVPNService, as device is not connected and setting_disable_netchange is true",
                    i = DNSVpnService.getDestroyIntent(accessContext(), accessContext().getString(R.string.reason_stop_network_change)));
            accessContext().startService(i);
            return;
        }
        //if (!connected || type == ConnectivityManager.TYPE_BLUETOOTH || type == ConnectivityManager.TYPE_DUMMY || type == ConnectivityManager.TYPE_VPN)
        if(!connected || (connectionType != ConnectionType.WIFI && connectionType != ConnectionType.MOBILE)) {
            return;
        }
        if (!serviceThreadRunning) {
            if (connectionType == ConnectionType.WIFI && autoWifi) {
                LogFactory.writeMessage(accessContext(), LOG_TAG, "Connected to WIFI and setting_auto_wifi is true. Starting Service..");
                startService();
            } else if (connectionType == ConnectionType.MOBILE && autoMobile) {
                LogFactory.writeMessage(accessContext(), LOG_TAG, "Connected to MOBILE and setting_auto_mobile is true. Starting Service..");
                startService();
            }
        }else {
            if (!(connectionType == ConnectionType.WIFI && autoWifi) && !(connectionType == ConnectionType.MOBILE && autoMobile) && Preferences.getInstance(accessContext()).getBoolean( "setting_disable_netchange", false) && serviceRunning) {
                LogFactory.writeMessage(accessContext(), LOG_TAG,
                        "Not on WIFI or MOBILE and setting_disable_netchange is true. Destroying DNSVPNService.",
                        i =DNSVpnService.getDestroyIntent(accessContext(), accessContext().getString(R.string.reason_stop_network_change)));
                accessContext().startService(i);
            }
        }
    }

    private enum ConnectionType{
        MOBILE,WIFI,VPN,OTHER
    }

    /*
      Here's the catch for this messy code: I keep getting crash reports (NPE) indicating that the context is null.
      However this shouldn't be possible because the variable running is set to false and every receiver is unregistered.
      So this method is used for accessing the context, the exception is thrown all up to the highest layer where it is gracefully ignored.
     */
    private Context accessContext() throws ReallyWeiredExceptionOnlyAFewPeopleHave{
        if(context == null)throw new ReallyWeiredExceptionOnlyAFewPeopleHave();
        return context;
    }
    
    private class ReallyWeiredExceptionOnlyAFewPeopleHave extends Exception{
        ReallyWeiredExceptionOnlyAFewPeopleHave(){
            super("It's strange, isn't it?");
        }
    }
}
