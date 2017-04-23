package com.frostnerd.dnschanger.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.utils.preferences.Preferences;

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
        final String dns1 = i.getStringExtra("dns1"), dns2 = i.getStringExtra("dns2"),
                dns1v6 = i.getStringExtra("dns1v6"), dns2v6 = i.getStringExtra("dns2v6");
        if(Preferences.getBoolean(this, "shortcut_click_override_settings", false)){
            Preferences.put(this, "dns1",dns1);
            Preferences.put(this, "dns2", dns2);
            Preferences.put(this, "dns1-v6", dns1v6);
            Preferences.put(this, "dns2-v6", dns2v6);
        }
        LogFactory.writeMessage(this, LOG_TAG, "DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6: " + dns1v6 + ", DNS2V6: " + dns2v6);
        if(API.isServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service is already running");
            if(Preferences.getBoolean(this, "shortcut_click_again_disable",false)){
                LogFactory.writeMessage(this, LOG_TAG, "shortcut_click_again_disable is true. Checking if service was started via same shortcut");
                LogFactory.writeMessage(this, LOG_TAG, "Binding to service");
                bindService(DNSVpnService.getBinderIntent(this), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        DNSVpnService service = ((DNSVpnService.ServiceBinder)binder).getService();
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Connected to service");
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Started via shortcut: " + service.startedFromShortcut());
                        if(service.startedFromShortcut() && service.getCurrentDNS1().equals(dns1) && service.getCurrentDNS2().equals(dns2)
                                && service.getCurrentDNS1V6().equals(dns1v6) && service.getCurrentDNS2V6().equals(dns2v6)){
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service was started via same shortcut. Stopping.");
                            unbindService(this);
                            startService(new Intent(ShortcutActivity.this, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(),true));
                            finish();
                        }else{
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service wasn't started using this shortcut");
                            unbindService(this);
                            start(dns1, dns2, dns1v6, dns2v6);
                        }
                        service = null;
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                },0);
            }else{
                LogFactory.writeMessage(this, LOG_TAG, "shortcut_click_again_disable is false");
                LogFactory.writeMessage(this, LOG_TAG, "Destroying service to be safe");
                LogFactory.writeMessage(this, LOG_TAG, "Destroy command sent");
                start(dns1, dns2, dns1v6, dns2v6);
            }
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Service not running. No need to destroy first");
            start(dns1, dns2, dns1v6, dns2v6);
        }
    }

    private void start(String dns1, String dns2, String dns1v6, String dns2v6){
        if(API.isServiceRunning(this)) {
            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Stopping Service to be safe");
            startService(DNSVpnService.getDestroyIntent(this));
        }
        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Starting BackgroundVpnConfigureActivity");
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
