package com.frostnerd.dnschangertests;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;
import android.system.OsConstants;

import com.frostnerd.utils.networking.NetworkUtil;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TestVPNService extends VpnService implements Runnable {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Builder builder = new Builder();
        builder.addAddress("192.168.0.10", 24);
        builder.addAddress(NetworkUtil.randomLocalIPv6Address(), 48);
        builder.addDnsServer("8.8.8.8");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setBlocking(true);
        }
        builder.setSession("DNS Test");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.allowFamily(OsConstants.AF_INET);
            builder.allowFamily(OsConstants.AF_INET6);
        }
        ParcelFileDescriptor fd = builder.establish();
        new Thread(this).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        try {
            while (true) Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
