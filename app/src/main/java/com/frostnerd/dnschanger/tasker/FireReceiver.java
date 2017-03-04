package com.frostnerd.dnschanger.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.frostnerd.dnschanger.BackgroundVpnConfigureActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
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
            String dns1 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1), dns2 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2),
                    dns1v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6), dns2v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6);
            BackgroundVpnConfigureActivity.startWithFixedDNS(context, dns1, dns2, dns1v6, dns2v6, true);
        }
    }
}
