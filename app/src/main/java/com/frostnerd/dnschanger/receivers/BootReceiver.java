package com.frostnerd.dnschanger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.services.ConnectivityJob;
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
        LogFactory.writeMessage(context, LOG_TAG, "Received an intent ", intent);
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)){
            LogFactory.writeMessage(context, LOG_TAG, "Action is BOOT_COMPLETED");
            LogFactory.writeMessage(context, LOG_TAG, "Starting ConnectivityBackgroundService");
            FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
            Job job = dispatcher.newJobBuilder().setService(ConnectivityJob.class)
                    .setTag("connectivity-job").setLifetime(Lifetime.FOREVER).setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                    .setRecurring(true).setReplaceCurrent(true).setTrigger(Trigger.executionWindow(0, 0)).build();
            dispatcher.mustSchedule(job);
            Preferences.put(context, "everything_disabled", false);
            if(Preferences.getBoolean(context,"setting_start_boot",false)){
                LogFactory.writeMessage(context, LOG_TAG, "User wants App to start on boot");
                Intent i = VpnService.prepare(context);
                LogFactory.writeMessage(context, LOG_TAG, "VPNService Prepare Intent", i);
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context,true);
            }
        }
    }
}
