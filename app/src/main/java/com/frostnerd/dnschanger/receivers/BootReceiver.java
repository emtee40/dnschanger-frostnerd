package com.frostnerd.dnschanger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.frostnerd.dnschanger.API.VPNServiceArguments;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.services.ConnectivityBackgroundService;
import com.frostnerd.dnschanger.services.DNSVpnService;
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
public class BootReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "[BootReceiver]";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Starting ConnectivityBackgroundService");
        context.startService(new Intent(context, ConnectivityBackgroundService.class));
        LogFactory.writeMessage(context, LOG_TAG, "Received an intent ", intent);
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)){
            LogFactory.writeMessage(context, LOG_TAG, "Action is BOOT_COMPLETED");
            if(Preferences.getBoolean(context,"setting_start_boot",false)){
                LogFactory.writeMessage(context, LOG_TAG, "User wants App to start on boot");
                Intent i = VpnService.prepare(context);
                LogFactory.writeMessage(context, LOG_TAG, "VPNService Prepare Intent", i);
                if(i == null){
                    LogFactory.writeMessage(context, LOG_TAG, "VPNService is prepared. Starting DNSVpnService",
                            i = DNSVpnService.getStartVPNIntent(context));
                    context.startService(i);
                }else{
                    LogFactory.writeMessage(context, LOG_TAG, "VPNService is NOT prepared. Starting BackgroundVpnConfigureActivity.");
                    BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
                }
            }
        }
    }
}
