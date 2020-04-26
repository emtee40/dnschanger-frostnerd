package com.frostnerd.dnschanger.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.dnschanger.util.Preferences;

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
public class ShortcutActivity extends AppCompatActivity {
    private static final String LOG_TAG = "[ShortcutActivity]";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        LogFactory.writeMessage(this, LOG_TAG, "Activity created", i);
        ArrayList<IPPortPair> upstreamServers = null;
        if(i.hasExtra("servers")){
            upstreamServers = Util.serializableFromString(i.getStringExtra("servers"));
        }
        if(!i.hasExtra("servers") || upstreamServers == null){
            upstreamServers = new ArrayList<>();
            String dns1 = i.getStringExtra("dns1"), dns2 = i.getStringExtra("dns2"),
                    dns1v6 = i.getStringExtra("dns1v6"), dns2v6 = i.getStringExtra("dns2v6");
            if(!TextUtils.isEmpty(dns1))upstreamServers.add(new IPPortPair(dns1, 53, false));
            if(!TextUtils.isEmpty(dns2))upstreamServers.add(new IPPortPair(dns2, 53, false));
            if(!TextUtils.isEmpty(dns1v6))upstreamServers.add(new IPPortPair(dns1v6, 53, true));
            if(!TextUtils.isEmpty(dns2v6))upstreamServers.add(new IPPortPair(dns2v6, 53, true));
        }
        if(upstreamServers.isEmpty()) {
            IPPortPair dns1 = PreferencesAccessor.Type.DNS1.getPair(this), dns2 = PreferencesAccessor.Type.DNS2.getPair(this),
                    dns1v6 = PreferencesAccessor.Type.DNS1_V6.getPair(this), dns2v6 = PreferencesAccessor.Type.DNS2_V6.getPair(this);
            if(!dns1.isEmpty()) upstreamServers.add(dns1);
            if(!dns2.isEmpty()) upstreamServers.add(dns2);
            if(!dns1v6.isEmpty()) upstreamServers.add(dns1v6);
            if(!dns2v6.isEmpty()) upstreamServers.add(dns2v6);
        }
        LogFactory.writeMessage(this, LOG_TAG, upstreamServers.toString());
        if(Util.isServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service is already running");
            if(Preferences.getInstance(this).getBoolean( "shortcut_click_again_disable",false)){
                LogFactory.writeMessage(this, LOG_TAG, "shortcut_click_again_disable is true. Checking if service was started via same shortcut");
                LogFactory.writeMessage(this, LOG_TAG, "Binding to service");
                final ArrayList<IPPortPair> finalUpstreamServers = upstreamServers;
                bindService(DNSVpnService.getBinderIntent(this), new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        DNSVpnService service = ((DNSVpnService.ServiceBinder)binder).getService();
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Connected to service");
                        LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Started via shortcut: " + service.wasStartedFromShortcut());
                        if(service.wasStartedFromShortcut() && service.addressesMatch(
                                finalUpstreamServers)){
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service was started via same shortcut. Stopping.");
                            unbindService(this);
                            startService(new Intent(ShortcutActivity.this, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(),true));
                            finish();
                        }else{
                            LogFactory.writeMessage(ShortcutActivity.this, LOG_TAG, "Service wasn't started using this shortcut");
                            unbindService(this);
                            start(finalUpstreamServers);
                        }
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
                if(!service.addressesMatch(servers)){
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
