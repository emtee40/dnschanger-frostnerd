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
import com.frostnerd.dnschanger.services.DNSVpnService;
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
public class BackgroundVpnConfigureActivity extends AppCompatActivity {
    private boolean startService = false;
    private static final int REQUEST_CODE = 112;
    private AlertDialog dialog1, dialog2;
    private long requestTime;
    private Intent serviceIntent;
    private boolean startedWithTasker;
    private static String LOG_TAG = "[BackgroundVpnConfigureActivity]";

    public static void startBackgroundConfigure(Context context, boolean startService) {
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] Starting Background configuring. Starting service: " + startService);
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("startService", startService).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void startWithFixedDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6, boolean startedWithTasker) {
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] Starting with fixed DNS. Started with tasker: " +startedWithTasker);
        LogFactory.writeMessage(context, LOG_TAG, "[STATIC] DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6: " + dns1v6 + ", DNS2V6: " + dns2v6);
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("fixeddns", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("dns1", dns1).putExtra("dns2", dns2).putExtra("dns1-v6", dns1v6).putExtra("dns2-v6", dns2v6).putExtra("startService", true).putExtra("startedWithTasker", startedWithTasker));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity", getIntent());
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        Intent intent = getIntent();
        final Intent conf = VpnService.prepare(this);
        startService = intent != null && intent.getBooleanExtra("startService", false);
        serviceIntent = new Intent(this, DNSVpnService.class).putExtra("start_vpn", true);
        LogFactory.writeMessage(this, LOG_TAG, "VPNService prepare", conf);
        LogFactory.writeMessage(this, LOG_TAG, "Starting Service: " + startService);
        if (intent != null && intent.getBooleanExtra("fixeddns", false)) {
            LogFactory.writeMessage(this, LOG_TAG, "Intent is not null and fixeddns is false");
            String dns1 = "8.8.8.8";
            String dns2 = "8.8.4.4";
            String dns1_v6 = "2001:4860:4860::8888";
            startedWithTasker = intent.getBooleanExtra("startedWithTasker", false);
            String dns2_v6 = "2001:4860:4860::8844";
            if (intent.hasExtra("dns1")) dns1 = intent.getStringExtra("dns1");
            if (intent.hasExtra("dns2")) dns2 = intent.getStringExtra("dns2");
            if (intent.hasExtra("dns1-v6")) dns1_v6 = intent.getStringExtra("dns1-v6");
            if (intent.hasExtra("dns2-v6")) dns2_v6 = intent.getStringExtra("dns2-v6");
            serviceIntent = serviceIntent.putExtra("fixeddns", true).putExtra("dns1", dns1).putExtra("dns2", dns2)
                    .putExtra("dns1-v6", dns1_v6).putExtra("dns2-v6", dns2_v6).putExtra("startedWithTasker", startedWithTasker);
            LogFactory.writeMessage(this, LOG_TAG, "ServiceIntent created", serviceIntent);
        }
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
                startService(serviceIntent);
            }
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showDialog(DialogInterface.OnClickListener click) {
        LogFactory.writeMessage(this, LOG_TAG, "Showing VPN Request Info Dialog");
        dialog1 = new AlertDialog.Builder(this).setTitle(getString(R.string.information) + " - " + getString(R.string.app_name)).setMessage(R.string.vpn_explain)
                .setCancelable(false).setPositiveButton(R.string.ok, click).show();
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
                    startService(serviceIntent);
                }
                setResult(RESULT_OK);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                LogFactory.writeMessage(this, LOG_TAG, "Action was cancelled");
                setResult(RESULT_CANCELED);
                if (System.currentTimeMillis() - requestTime <= 750) {//Most likely the system
                    LogFactory.writeMessage(this, LOG_TAG, "Looks like the System cancelled the action, not the User");
                    LogFactory.writeMessage(this, LOG_TAG, "Showing dialog which explains that this is most likely the System");
                    dialog2 = new AlertDialog.Builder(this).setTitle(getString(R.string.app_name) + " - " + getString(R.string.information)).setMessage(R.string.background_configure_error).setPositiveButton(R.string.open_app, new DialogInterface.OnClickListener() {
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
                    }).show();
                    LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
                } else finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
