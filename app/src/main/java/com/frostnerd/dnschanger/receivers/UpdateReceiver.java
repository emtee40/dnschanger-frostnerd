package com.frostnerd.dnschanger.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.frostnerd.dnschanger.activities.BackgroundVpnConfigureActivity;
import com.frostnerd.dnschanger.util.Preferences;

/*
 * Copyright (C) 2020 Daniel Wolf (Ch4t4r)
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
class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            boolean startAfterUpdate = Preferences.getInstance(context).getBoolean("setting_start_after_update", false);
            if (!startAfterUpdate) return;
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_MY_PACKAGE_REPLACED)) {
                BackgroundVpnConfigureActivity.startBackgroundConfigure(context, true);
            } else if (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED)) {
                if (intent.getData() != null && intent.getData().getSchemeSpecificPart().equalsIgnoreCase(context.getPackageName())) {
                    BackgroundVpnConfigureActivity.startBackgroundConfigure(context, true);
                }
            }
        }
    }
}
