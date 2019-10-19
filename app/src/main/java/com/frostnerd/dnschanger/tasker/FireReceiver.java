package com.frostnerd.dnschanger.tasker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.Util;

import java.util.ArrayList;

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
public class FireReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "[FireReceiver]";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Helper.ACTION_FIRE_SETTINGS.equals(intent.getAction()))return;
        LogFactory.writeMessage(context, LOG_TAG, "FireReceiver called", intent);
        Helper.scrub(intent);
        final Bundle bundle = intent.getBundleExtra(Helper.EXTRA_BUNDLE);
        Helper.scrub(bundle);
        if(Helper.isBundleValid(context, bundle)){
            LogFactory.writeMessage(context, LOG_TAG, "Got Tasker action");
            if(bundle.containsKey(Helper.BUNDLE_EXTRA_STOP_DNS)){
                Intent i;
                LogFactory.writeMessage(context, LOG_TAG, "Action: Stop DNS",
                        i = DNSVpnService.getDestroyIntent(context, context.getString(R.string.reason_stop_tasker)));
                if(Util.isServiceRunning(context))context.startService(i);
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_PAUSE_DNS)){
                Intent i;
                LogFactory.writeMessage(context, LOG_TAG, "Action: Pause DNS",
                        i = DNSVpnService.getStopVPNIntent(context));
                if(Util.isServiceRunning(context))context.startService(i);
            }else if(bundle.containsKey(Helper.BUNDLE_EXTRA_RESUME_DNS)){
                LogFactory.writeMessage(context, LOG_TAG, "Action: Resume DNS");
                LogFactory.writeMessage(context, LOG_TAG, "Starting BackgroundVpnConfigureActivity");
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
            }else{
                LogFactory.writeMessage(context, LOG_TAG, "Action: Start DNS");
                ArrayList<IPPortPair> servers;
                servers = new ArrayList<>();

                String dns1 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1), dns2 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2),
                        dns1v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6), dns2v6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6);
                LogFactory.writeMessage(context, LOG_TAG, "DNS 1: " + dns1 + "; DNS 2: " + dns2 +
                "; DNSV6 1: " + dns1v6 + "; DNSV6 2: " + dns2v6);
                if (bundle.containsKey(Helper.BUNDLE_EXTRA_V2)) {
                    LogFactory.writeMessage(context, LOG_TAG, "The bundle is Version 2");
                    if (!TextUtils.isEmpty(dns1)) servers.add(IPPortPair.wrap(dns1));
                    if (!TextUtils.isEmpty(dns2)) servers.add(IPPortPair.wrap(dns2));
                    if (!TextUtils.isEmpty(dns1v6)) servers.add(IPPortPair.wrap(dns1v6));
                    if (!TextUtils.isEmpty(dns2v6)) servers.add(IPPortPair.wrap(dns2v6));
                } else {
                    LogFactory.writeMessage(context, LOG_TAG, "The bundle is Version 1");
                    if (!TextUtils.isEmpty(dns1)) servers.add(new IPPortPair(dns1, 53, false));
                    if (!TextUtils.isEmpty(dns2)) servers.add(new IPPortPair(dns2, 53, false));
                    if (!TextUtils.isEmpty(dns1v6)) servers.add(new IPPortPair(dns1v6, 53, false));
                    if (!TextUtils.isEmpty(dns2v6)) servers.add(new IPPortPair(dns2v6, 53, false));
                }
                LogFactory.writeMessage(context, LOG_TAG, "Servers: " + servers.toString());
                LogFactory.writeMessage(context, LOG_TAG, "Starting BackgroundVpnConfigureActivity");
                BackgroundVpnConfigureActivity.startWithFixedDNS(context, servers, true);
            }
        }else LogFactory.writeMessage(context, LOG_TAG, "Bundle is invalid");
    }
}
