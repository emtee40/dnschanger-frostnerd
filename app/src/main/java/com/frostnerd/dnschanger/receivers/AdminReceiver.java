package com.frostnerd.dnschanger.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;

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
public class AdminReceiver extends DeviceAdminReceiver {
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