package com.frostnerd.dnschanger;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
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

    public static void startBackgroundConfigure(Context context, boolean startService) {
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("startService", startService).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void startWithFixedDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6, boolean startedWithTasker) {
        context.startActivity(new Intent(context, BackgroundVpnConfigureActivity.class).putExtra("fixeddns", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("dns1", dns1).putExtra("dns2", dns2).putExtra("dns1-v6", dns1v6).putExtra("dns2-v6", dns2v6).putExtra("startService", true).putExtra("startedWithTasker", startedWithTasker));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        Intent intent = getIntent();
        final Intent conf = VpnService.prepare(this);
        startService = intent != null && intent.getBooleanExtra("startService", false);
        serviceIntent = new Intent(this, DNSVpnService.class).putExtra("start_vpn", true);
        if (intent != null && intent.getBooleanExtra("fixeddns", false)) {
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
        }
        if (conf != null) {
            showDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    requestTime = System.currentTimeMillis();
                    startActivityForResult(conf, REQUEST_CODE);
                }
            });
        } else {
            if (startService) startService(serviceIntent);
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showDialog(DialogInterface.OnClickListener click) {
        dialog1 = new AlertDialog.Builder(this).setTitle(getString(R.string.information) + " - " + getString(R.string.app_name)).setMessage(R.string.vpn_explain)
                .setCancelable(false).setPositiveButton(R.string.ok, click).show();
    }

    @Override
    protected void onDestroy() {
        if (dialog1 != null) dialog1.cancel();
        if (dialog2 != null) dialog2.cancel();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (startService)
                    startService(serviceIntent);
                setResult(RESULT_OK);
                finish();
            } else if (resultCode == RESULT_CANCELED) {
                setResult(RESULT_CANCELED);
                if (System.currentTimeMillis() - requestTime <= 750) {//Most likely the system
                    dialog2 = new AlertDialog.Builder(this).setTitle(getString(R.string.app_name) + " - " + getString(R.string.information)).setMessage(R.string.background_configure_error).setPositiveButton(R.string.open_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(BackgroundVpnConfigureActivity.this, MainActivity.class));
                            finish();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            finish();
                        }
                    }).show();
                } else finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
