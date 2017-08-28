package com.frostnerd.dnschanger.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class _AdminReceiver extends DeviceAdminReceiver {
    private static final String LOG_TAG = "[AdminReceiver]";

    @Override
    public void onEnabled(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Admin enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Disable requested");
        return context.getString(R.string.device_admin_removal_warning);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Admin disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        LogFactory.writeMessage(context, LOG_TAG, "Password changed");
    }
}