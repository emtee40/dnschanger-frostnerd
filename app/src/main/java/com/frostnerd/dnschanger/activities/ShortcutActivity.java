package com.frostnerd.dnschanger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.DNSVpnService;

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
    private static final String LOG_TAG = "[ShortcutActivity]";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        LogFactory.writeMessage(this, LOG_TAG, "Activity created", i);
        String dns1 = i.getStringExtra("dns1"), dns2 = i.getStringExtra("dns2"),
                dns1v6 = i.getStringExtra("dns1v6"), dns2v6 = i.getStringExtra("dns2v6");
        LogFactory.writeMessage(this, LOG_TAG, "DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6: " + dns1v6 + ", DNS2V6: " + dns2v6);
        LogFactory.writeMessage(this, LOG_TAG, "Starting BackgroundVPNConfigureActivity");
        if(API.checkVPNServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Destroying service to be safe");
            startService(new Intent(this, DNSVpnService.class).putExtra("destroy", true));
            LogFactory.writeMessage(this, LOG_TAG, "Destroy command sent");
        }else LogFactory.writeMessage(this, LOG_TAG, "Service not running. No need to destroy first");
        BackgroundVpnConfigureActivity.startWithFixedDNS(this,dns1,
                dns2,dns1v6, dns2v6,false);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogFactory.writeMessage(this, LOG_TAG, "Destroying");
    }
}
