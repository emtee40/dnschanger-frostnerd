package com.frostnerd.dnschanger;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class API {
    public static final String BROADCAST_SERVICE_STATUS_CHANGE = "com.frostnerd.dnschanger.VPN_SERVICE_CHANGE";
    public static final String BROADCAST_SERVICE_STATE_REQUEST = "com.frostnerd.dnschanger.VPN_STATE_CHANGE";
    public static String TAG = "Debug";

    public static boolean checkVPNServiceRunning(Context c){
        ActivityManager am = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for(ActivityManager.RunningServiceInfo service: am.getRunningServices(Integer.MAX_VALUE)){
            if(name.equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }
}
