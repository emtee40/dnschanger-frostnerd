package com.frostnerd.dnschanger.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.jobs.NetworkCheckHandle;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class ConnectivityBackgroundService extends Service {
    private NetworkCheckHandle handle;
    private static final String LOG_TAG = "[ConnectivityBackgroundService]";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Service created.");
        handle = new NetworkCheckHandle(this, LOG_TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogFactory.writeMessage(this, LOG_TAG, "Service destroyed.");
        handle.stop();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogFactory.writeMessage(this, LOG_TAG, "Task removed.");
    }
}
