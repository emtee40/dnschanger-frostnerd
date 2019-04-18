package com.frostnerd.dnschanger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.Preferences;

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
public class BootReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "[BootReceiver]";

    @Override
    public void onReceive(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Received an intent ", intent);
        if(intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)){
            LogFactory.writeMessage(context, LOG_TAG, "Action is BOOT_COMPLETED");
            LogFactory.writeMessage(context, LOG_TAG, "Starting ConnectivityBackgroundService");
            Util.runBackgroundConnectivityCheck(context);
            Preferences.getInstance(context).put( "everything_disabled", false);
            if(Preferences.getInstance(context).getBoolean("setting_start_boot",false)){
                LogFactory.writeMessage(context, LOG_TAG, "User wants App to start on boot");
                Intent i = VpnService.prepare(context);
                LogFactory.writeMessage(context, LOG_TAG, "VPNService Prepare Intent", i);
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
            }
        }
    }
}
