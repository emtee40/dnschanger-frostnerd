package com.frostnerd.dnschanger.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

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
        final ArrayList<IPPortPair> upstreamServers;
        if(i.hasExtra("servers"))upstreamServers = (ArrayList<IPPortPair>) i.getSerializableExtra("servers");
        else{
            upstreamServers = new ArrayList<>();
            String dns1 = i.getStringExtra("dns1"), dns2 = i.getStringExtra("dns2"),
                    dns1v6 = i.getStringExtra("dns1v6"), dns2v6 = i.getStringExtra("dns2v6");
            if(!TextUtils.isEmpty(dns1))upstreamServers.add(new IPPortPair(dns1, 53, false));
            if(!TextUtils.isEmpty(dns2))upstreamServers.add(new IPPortPair(dns2, 53, false));
            if(!TextUtils.isEmpty(dns1v6))upstreamServers.add(new IPPortPair(dns1v6, 53, true));
            if(!TextUtils.isEmpty(dns2v6))upstreamServers.add(new IPPortPair(dns2v6, 53, true));
        }
        LogFactory.writeMessage(this, LOG_TAG, upstreamServers.toString());
        if(Util.isServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service is already running");
            if(Preferences.getBoolean(this, "shortcut_click_again_disable",false)){
                LogFactory.writeMessage(this, LOG_TAG, "shortcut_click_again_disable is true. Checking if service was started via same shortcut");
                LogFactory.writeMessage(this, LOG_TAG, "Binding to service");
                bindService(DNSVpnService.getBinderIntent(this), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        DNSVpnService service = ((DNSVpnService.ServiceBinder)binder).getService();
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Connected to service");
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Started via shortcut: " + service.wasStartedFromShortcut());
                        if(service.wasStartedFromShortcut() && service.addresesMatch(upstreamServers)){
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service was started via same shortcut. Stopping.");
                            unbindService(this);
                            startService(new Intent(ShortcutActivity.this, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(),true));
                            finish();
                        }else{
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service wasn't started using this shortcut");
                            unbindService(this);
                            start(upstreamServers);
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
                start(upstreamServers);
            }
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Service not running. No need to destroy first");
            start(upstreamServers);
        }
    }

    private void start(final ArrayList<IPPortPair> servers){
        if(Util.isServiceRunning(this))bindService(DNSVpnService.getBinderIntent(this), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                DNSVpnService service = ((DNSVpnService.ServiceBinder)binder).getService();
                LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Connected to service");
                LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Started via shortcut: " + service.wasStartedFromShortcut());
                boolean threadRunning = Util.isServiceThreadRunning();
                if(!service.addresesMatch(servers)){
                    unbindService(this);
                    startService(DNSVpnService.getDestroyIntent(ShortcutActivity.this));
                    LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Starting BackgroundVpnConfigureActivity");
                    BackgroundVpnConfigureActivity.startWithFixedDNS(ShortcutActivity.this, servers, false);
                    finish();
                }else{
                    if(!threadRunning)startService(new Intent(ShortcutActivity.this, DNSVpnService.class)
                            .putExtra(VPNServiceArgument.COMMAND_START_VPN.toString(), true));
                    unbindService(this);
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        },0);
        else {
            BackgroundVpnConfigureActivity.startWithFixedDNS(this, servers,false);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogFactory.writeMessage(this, LOG_TAG, "Destroying");
    }
}
