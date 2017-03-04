package com.frostnerd.dnschanger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.utils.general.StringUtils;
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
    private boolean run = true, isRunning = false, stopped = false;
    private Thread thread;
    private ParcelFileDescriptor tunnelInterface;
    private Builder builder = new Builder();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 112;
    private Handler handler = new Handler();
    private String dns1,dns2,dns1_v6,dns2_v6;
    private boolean fixedDNS = false, startedWithTasker = false;
    private BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastServiceState(isRunning);
        }
    };

    @Override
    public void onDestroy() {
        stopped = true;
        run = false;
        if (thread != null) thread.interrupt();
        unregisterReceiver(stateRequestReceiver);
        thread = null;
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        super.onDestroy();
    }

    private void broadcastServiceState(boolean vpnRunning){
        sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running",vpnRunning).putExtra("started_with_tasker", startedWithTasker));
    }

    private void updateNotification() { //TODO Fix Bug: Actions are not properly removed
        initNotification();
        if(!Preferences.getBoolean(this, "setting_show_notification",false) && notificationManager != null)notificationManager.cancel(NOTIFICATION_ID);
        if(stopped || notificationBuilder == null || !Preferences.getBoolean(this, "setting_show_notification",false) || notificationManager == null)return;
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
        notificationBuilder.setSubText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        notificationBuilder.setContentTitle(getString(isRunning ? R.string.active : R.string.paused));
        notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().
                bigText("DNS 1: " + dns1 + "\nDNS 2: " + dns2 + "\nDNSV6 1: " + dns1_v6 + "\nDNSV6 2: " + dns2_v6));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(notificationManager != null && notificationBuilder != null && !stopped) notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        },10);
    }

    private void initNotification(){
        if (notificationBuilder == null) {
            notificationBuilder = new android.support.v7.app.NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_small_icon); //TODO Update Image
            notificationBuilder.setContentTitle(getString(R.string.app_name));
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));
            notificationBuilder.setAutoCancel(false);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setUsesChronometer(true);
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNotification();
        registerReceiver(stateRequestReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST));
    }

    public static void startWithSetDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6){
        context.startService(new Intent(context, DNSVpnService.class).putExtra("fixeddns",true).
                putExtra("dns1", dns1).putExtra("dns2", dns2).putExtra("dns1-v6", dns1v6).putExtra("dns2-v6", dns2v6));
    }

    private void updateDNSServers(Intent intent){
        if(fixedDNS){
            if(intent == null)return;
            if(intent.hasExtra("dns1"))dns1 = intent.getStringExtra("dns1");
            if(intent.hasExtra("dns2"))dns2 = intent.getStringExtra("dns2");
            if(intent.hasExtra("dns1-v6"))dns1_v6 = intent.getStringExtra("dns1-v6");
            if(intent.hasExtra("dns2-v6"))dns2_v6 = intent.getStringExtra("dns2-v6");
        }else{
            dns1 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.8.8");
            dns2 = Preferences.getString(DNSVpnService.this, "dns2", "8.8.4.4");
            dns1_v6 = Preferences.getString(DNSVpnService.this, "dns1-v6", "2001:4860:4860::8888");
            dns2_v6 = Preferences.getString(DNSVpnService.this, "dns2-v6", "2001:4860:4860::8844");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            fixedDNS = intent.hasExtra("fixeddns") ? intent.getBooleanExtra("fixeddns", false) : fixedDNS;
            startedWithTasker = intent.hasExtra("startedWithTasker") ? intent.getBooleanExtra("startedWithTasker", false) : startedWithTasker;
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
                updateDNSServers(intent);
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            initNotification();
                            if(notificationBuilder != null) notificationBuilder.setWhen(System.currentTimeMillis());
                            tunnelInterface = builder.setSession("DnsChanger").addAddress("192.168.0.1", 24).addDnsServer(dns1).addDnsServer(dns2)
                                    .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                            DatagramChannel tunnel = DatagramChannel.open();
                            tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                            protect(tunnel.socket());
                            isRunning = true;
                            broadcastServiceState(true);
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
                            broadcastServiceState(false);
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
                stopped = true;
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
                stopSelf();
            }
        }
        updateNotification();
        return START_STICKY;
    }
}
