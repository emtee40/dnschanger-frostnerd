package com.frostnerd.dnschanger.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.dialogs.VPNInfoDialog;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;

import java.util.ArrayList;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
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
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("fixeddns", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("servers", upstreamServers).putExtra("startService", true).putExtra("startedWithTasker", startedWithTasker));
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
                    startActivityForResult(conf, REQUEST_CODE);
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
