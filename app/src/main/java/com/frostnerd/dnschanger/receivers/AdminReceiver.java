package com.frostnerd.dnschanger.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.frostnerd.dnschanger.R;

public class AdminReceiver extends DeviceAdminReceiver {
        @Override
        public void onEnabled(Context context, Intent intent) {
        }

        @Override
        public CharSequence onDisableRequested(Context context, Intent intent) {
            return context.getString(R.string.device_admin_removal_warning);
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
        }

        @Override
        public void onPasswordChanged(Context context, Intent intent) {

        }
    }