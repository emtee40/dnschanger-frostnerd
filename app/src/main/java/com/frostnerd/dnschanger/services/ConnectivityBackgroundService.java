package com.frostnerd.dnschanger.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.jobs.NetworkCheckHandle;

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
        handle = new NetworkCheckHandle(getApplicationContext() == null ? this : getApplicationContext(), LOG_TAG);
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
