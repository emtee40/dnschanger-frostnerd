package com.frostnerd.dnschanger;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getSupportActionBar() != null)getSupportActionBar().hide();
        Intent i = getIntent();
        final Intent conf = VpnService.prepare(this);
        if (conf != null) {
            startService = i != null && conf.getBooleanExtra("startService", false);
            showDialog(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivityForResult(conf, REQUEST_CODE);
                }
            });
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void showDialog(DialogInterface.OnClickListener click) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.information) + " - " + getString(R.string.app_name)).setMessage(R.string.vpn_explain)
                .setCancelable(false).setPositiveButton(R.string.ok, click).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (startService)
                    startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn", true));
                setResult(RESULT_OK);
            } else if (resultCode == RESULT_CANCELED) setResult(RESULT_CANCELED);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
