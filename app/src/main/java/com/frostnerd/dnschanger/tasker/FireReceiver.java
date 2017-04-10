package com.frostnerd.dnschanger.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class FireReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "[FireReceiver]";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Helper.ACTION_FIRE_SETTINGS.equals(intent.getAction()))return;
        LogFactory.writeMessage(context, LOG_TAG, "FireReceiver called");
        Helper.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(Helper.EXTRA_BUNDLE);
        Helper.scrub(bundle);
        if(Helper.isBundleValid(bundle)){
            LogFactory.writeMessage(context, LOG_TAG, "Got Tasker action");
            if(bundle.containsKey(Helper.BUNDLE_EXTRA_STOP_DNS)){
                LogFactory.writeMessage(context, LOG_TAG, "Action: Stop DNS");
                context.startService(new Intent(context, DNSVpnService.class).putExtra("destroy",true));
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_PAUSE_DNS)){
                LogFactory.writeMessage(context, LOG_TAG, "Action: Pause DNS");
                context.startService(new Intent(context, DNSVpnService.class).putExtra("stop_vpn",true));
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_RESUME_DNS)){
                LogFactory.writeMessage(context, LOG_TAG, "Action: Resume DNS");
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
            }else{
                LogFactory.writeMessage(context, LOG_TAG, "Action: Start DNS");
                String dns1 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1), dns2 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2),
                        dns1v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6), dns2v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6);
                LogFactory.writeMessage(context, LOG_TAG, "DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6: " + dns1v6 + ", DNS2V6: " + dns2v6);
                LogFactory.writeMessage(context, LOG_TAG, "Starting BackgroundVpnConfigureActivity");
                BackgroundVpnConfigureActivity.startWithFixedDNS(context, dns1, dns2, dns1v6, dns2v6, true);
            }
        }else LogFactory.writeMessage(context, LOG_TAG, "Bundle is invalid");
    }
}
