package com.frostnerd.dnschanger;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
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
