package com.frostnerd.dnschanger.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.dialogs.VPNInfoDialog;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.ThemeHandler;
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
public class BackgroundVpnConfigureActivity extends AppCompatActivity {
    private boolean startService = false;
    private static final int REQUEST_CODE = 112;
    private AlertDialog dialog1, dialog2;
    private long requestTime;
    private Intent serviceIntent;
    private static final String LOG_TAG = "[BackgroundVpnConfigureActivity]";

    public static void startBackgroundConfigure(Context context, boolean startService) {
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] Starting Background configuring. Starting service: " + startService);
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("startService", startService).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void startWithFixedDNS(final Context context, ArrayList<IPPortPair> upstreamServers, boolean startedWithTasker) {
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] Starting with fixed DNS. Started with tasker: " +startedWithTasker);
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] " + upstreamServers);

        if(startedWithTasker) {
            Util.startService(context,
                    DNSVpnService.getStartVPNIntent(context, upstreamServers, true, true));
        } else {
            Intent intent = new Intent(context, BackgroundVpnConfigureActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("fixeddns", true)
                    .putExtra("servers", upstreamServers)
                    .putExtra("startService", true)
                    .putExtra("startedWithTasker", false);
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity", getIntent());
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        Intent intent = getIntent();
        final Intent conf = VpnService.prepare(this);
        startService = intent != null && intent.getBooleanExtra("startService", false);
        LogFactory.writeMessage(this, LOG_TAG, "VPNService prepare", conf);
        LogFactory.writeMessage(this, LOG_TAG, "Starting Service: " + startService);
        if (intent != null && intent.getBooleanExtra("fixeddns", false)) {
            LogFactory.writeMessage(this, LOG_TAG, "Intent is not null and fixeddns is false");
            ArrayList<IPPortPair> servers = (ArrayList<IPPortPair>) intent.getSerializableExtra("servers");
            boolean startedWithTasker = intent.getBooleanExtra("startedWithTasker", false);
            serviceIntent = DNSVpnService.getStartVPNIntent(this, servers, startedWithTasker,intent.getBooleanExtra("fixeddns", false));
            LogFactory.writeMessage(this, LOG_TAG, "ServiceIntent created", serviceIntent);
        }else serviceIntent = DNSVpnService.getStartVPNIntent(this);
        if (conf != null) {
            LogFactory.writeMessage(this, LOG_TAG, "VPN access not yet granted. Requesting access (Showing Info dialog).");
            showDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LogFactory.writeMessage(BackgroundVpnConfigureActivity.this, LOG_TAG, "User clicked OK in Request Info Dialog. Requesting access now.");
                    requestTime = System.currentTimeMillis();
                    LogFactory.writeMessage(BackgroundVpnConfigureActivity.this, LOG_TAG, "Preparing VPNService", conf);
                    try {
                        startActivityForResult(conf, REQUEST_CODE);
                    } catch (ActivityNotFoundException e) {
                        finish();
                    }
                }
            });
        } else {
            LogFactory.writeMessage(this, LOG_TAG, "Access to VPN was already granted.");
            if (startService){
                LogFactory.writeMessage(this, LOG_TAG, "Starting DNSVPNService");
                Util.startService(this,serviceIntent);
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showDialog(DialogInterface.OnClickListener click) {
        LogFactory.writeMessage(this, LOG_TAG, "Showing VPN Request Info Dialog");
        new VPNInfoDialog(this, click);
        LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
    }

    @Override
    protected void onDestroy() {
        if (dialog1 != null) dialog1.cancel();
        if (dialog2 != null) dialog2.cancel();
        LogFactory.writeMessage(this, LOG_TAG, "Destroying");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            LogFactory.writeMessage(this, LOG_TAG, "Got result for VPN Request");
            if (resultCode == RESULT_OK) {
                LogFactory.writeMessage(this, LOG_TAG, "Access was granted");
                if (startService){
                    LogFactory.writeMessage(this, LOG_TAG, "Starting service", serviceIntent);
                    Util.startService(this,serviceIntent);
                }
                setResult(RESULT_OK);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                LogFactory.writeMessage(this, LOG_TAG, "Action was cancelled");
                setResult(RESULT_CANCELED);
                if (System.currentTimeMillis() - requestTime <= 750) {//Most likely the system
                    LogFactory.writeMessage(this, LOG_TAG, "Looks like the System cancelled the action, not the User");
                    LogFactory.writeMessage(this, LOG_TAG, "Showing dialog which explains that this is most likely the System");
                    dialog2 = new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(BackgroundVpnConfigureActivity.this)).setTitle(getString(R.string.app_name) + " - " + getString(R.string.information)).setMessage(R.string.background_configure_error).setPositiveButton(R.string.open_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i;
                            LogFactory.writeMessage(BackgroundVpnConfigureActivity.this, LOG_TAG, "Redirecting User to PinActivity",
                                    i = new Intent(BackgroundVpnConfigureActivity.this, PinActivity.class));
                            startActivity(i);
                            finish();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(BackgroundVpnConfigureActivity.this, LOG_TAG, "User choose to cancel the action.");
                            dialog.cancel();
                            finish();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    }).show();
                    LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
                } else finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
