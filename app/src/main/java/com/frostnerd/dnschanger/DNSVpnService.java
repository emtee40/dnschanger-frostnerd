package com.frostnerd.dnschanger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.utils.general.StringUtils;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Random;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DNSVpnService extends VpnService {
    private boolean run = true, isRunning = false;
    private Thread thread;
    private ParcelFileDescriptor tunnelInterface;
    private Builder builder = new Builder();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 112;
    private Handler handler = new Handler();

    @Override
    public void onDestroy() {
        run = false;
        if (thread != null) thread.interrupt();
        thread = null;
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        super.onDestroy();
    }

    private void updateNotification() { //TODO Fix Bug: Actions are not properly removed
        if(notificationBuilder == null || !Preferences.getBoolean(this, "setting_show_notification",false) || notificationManager == null)return;
        notificationBuilder.mActions.clear();
        android.support.v4.app.NotificationCompat.Action a1 = new NotificationCompat.Action(isRunning ? R.drawable.ic_stat_pause : R.drawable.ic_stat_resume,
                getString(isRunning ? R.string.action_pause : R.string.action_resume),
                PendingIntent.getService(this, 0, new Intent(this, DNSVpnService.class).setAction(new Random().nextInt(50) + "_action")
                        .putExtra(isRunning ? "stop_vpn" : "start_vpn", true), PendingIntent.FLAG_CANCEL_CURRENT)),
        a2 = new android.support.v4.app.NotificationCompat.Action(R.drawable.ic_stat_stop,
                getString(R.string.action_stop), PendingIntent.getService(this, 1, new Intent(this, DNSVpnService.class)
                .setAction(StringUtils.randomString(80) + "_action").putExtra("destroy", true), PendingIntent.FLAG_CANCEL_CURRENT));
        notificationBuilder.addAction(a1);
        notificationBuilder.addAction(a2);
        notificationBuilder.setContentText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        notificationBuilder.setContentTitle(getString(isRunning ? R.string.active : R.string.paused));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(notificationManager != null && notificationBuilder != null) notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        },10);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (notificationBuilder == null) {
            notificationBuilder = new android.support.v7.app.NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher); //TODO Update Image
            notificationBuilder.setContentTitle(getString(R.string.app_name));
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
            notificationBuilder.setAutoCancel(false);
            notificationBuilder.setOngoing(true);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("stop_vpn", false)) {
            if (thread != null) {
                run = false;
                thread.interrupt();
                thread = null;
            }
        } else if (intent.getBooleanExtra("start_vpn", false)) {
            if (thread != null) {
                run = false;
                thread.interrupt();
            }
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String dns1 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.8.8"),
                                dns2 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.4.4"),
                        dns1_v6 = Preferences.getString(DNSVpnService.this, "dns1-v6", "2001:4860:4860::8888"),
                        dns2_v6 = Preferences.getString(DNSVpnService.this, "dns2-v6", "2001:4860:4860::8844");
                        tunnelInterface = builder.setSession("DnsChanger").addAddress("192.168.0.1", 24).addDnsServer(dns1).addDnsServer(dns2)
                                .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                        DatagramChannel tunnel = DatagramChannel.open();
                        tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                        protect(tunnel.socket());
                        isRunning = true;
                        sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running",true));
                        updateNotification();
                        try {
                            while (run) {
                                Thread.sleep(100);
                            }
                        } catch (InterruptedException e2) {

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        isRunning = false;
                        sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running",false));
                        updateNotification();
                        if (tunnelInterface != null) try {
                            tunnelInterface.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            run = true;
            thread.start();
        }else if(intent.getBooleanExtra("destroy",false)){
            if (thread != null) {
                run = false;
                thread.interrupt();
            }
            stopSelf();
        }
        updateNotification();
        return START_STICKY;
    }
}
