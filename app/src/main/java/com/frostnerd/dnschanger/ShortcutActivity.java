package com.frostnerd.dnschanger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class ShortcutActivity extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        BackgroundVpnConfigureActivity.startWithFixedDNS(this,i.getStringExtra("dns1"),
                i.getStringExtra("dns2"),i.getStringExtra("dns1v6"), i.getStringExtra("dns2v6"),false);
        finish();
    }
}
