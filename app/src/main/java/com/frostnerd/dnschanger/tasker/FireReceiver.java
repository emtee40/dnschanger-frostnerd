package com.frostnerd.dnschanger.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.frostnerd.dnschanger.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.DNSVpnService;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Helper.ACTION_FIRE_SETTINGS.equals(intent.getAction()))return;
        Helper.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(Helper.EXTRA_BUNDLE);
        Helper.scrub(bundle);
        if(Helper.isBundleValid(bundle)){
            if(bundle.containsKey(Helper.BUNDLE_EXTRA_STOP_DNS)){
                context.startService(new Intent(context, DNSVpnService.class).putExtra("destroy",true));
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_PAUSE_DNS)){
                context.startService(new Intent(context, DNSVpnService.class).putExtra("stop_vpn",true));
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_RESUME_DNS)){
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
            }else{
                String dns1 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1), dns2 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2),
                        dns1v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6), dns2v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6);
                BackgroundVpnConfigureActivity.startWithFixedDNS(context, dns1, dns2, dns1v6, dns2v6, true);
            }
        }
    }
}
