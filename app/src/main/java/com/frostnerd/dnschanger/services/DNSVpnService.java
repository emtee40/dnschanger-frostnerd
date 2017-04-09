package com.frostnerd.dnschanger.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.service.quicksettings.TileService;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.tiles.TilePause;
import com.frostnerd.dnschanger.tiles.TileResume;
import com.frostnerd.dnschanger.tiles.TileStart;
import com.frostnerd.dnschanger.tiles.TileStop;
import com.frostnerd.utils.general.StringUtils;
import com.frostnerd.utils.preferences.Preferences;
import com.frostnerd.utils.stats.AppTaskGetter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
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
    private boolean fixedDNS = false, startedWithTasker = false, autoPaused = false;
    private BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            broadcastServiceState(isRunning);
        }
    };
    private Set<String> autoPauseApps;
    private Runnable autoPausedRestartRunnable = new Runnable() {
        @Override
        public void run() {
            int counter = 0;
                try {
                    while(!stopped){
                        if(counter >= 4){
                            if(!autoPauseApps.contains(AppTaskGetter.getMostRecentApp(DNSVpnService.this,1000*1000))){
                                startService(new Intent(DNSVpnService.this, DNSVpnService.class).putExtra("start_vpn",true));
                                break;
                            }
                            counter = 0;
                        }
                        Thread.sleep(250);
                        counter++;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    };
    private boolean wasCrashShownToUser = false;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if(!wasCrashShownToUser){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                startActivity(new Intent(DNSVpnService.this, ErrorDialogActivity.class).putExtra("stacktrace",sw.toString()));
                wasCrashShownToUser = true;
            }
            stopped = true;
            run = false;
            stopSelf();
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
        updateTiles(this);
    }

    private void broadcastServiceState(boolean vpnRunning){
        sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).putExtra("vpn_running",vpnRunning).putExtra("started_with_tasker", startedWithTasker));
    }

    private void updateNotification() { //Well, this method is a mess.
        initNotification();
        if(!Preferences.getBoolean(this, "setting_show_notification",false) && notificationManager != null){
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }
        if(stopped || notificationBuilder == null || !Preferences.getBoolean(this, "setting_show_notification",false) || notificationManager == null)return;
        boolean pinProtected = Preferences.getBoolean(this, "pin_notification",false);
        android.support.v4.app.NotificationCompat.Action a1 = notificationBuilder.mActions.get(0);
        a1.icon = isRunning ? R.drawable.ic_stat_pause : R.drawable.ic_stat_resume;
        a1.title = getString(isRunning ? R.string.action_pause : R.string.action_resume);
        a1.actionIntent = pinProtected ? PendingIntent.getActivity(this,0,new Intent(this, PinActivity.class).setAction(new Random().nextInt(50) + "_action").
                putExtra(isRunning ? "stop_vpn" : "start_vpn", true).putExtra("redirectToService",true), PendingIntent.FLAG_UPDATE_CURRENT) :
                PendingIntent.getService(this, 0, new Intent(this, DNSVpnService.class).setAction(new Random().nextInt(50) + "_action").putExtra(isRunning ? "stop_vpn" : "start_vpn", true), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.mActions.get(1).actionIntent = pinProtected ? PendingIntent.getActivity(this,0,new Intent(this, PinActivity.class).
                setAction(new Random().nextInt(50) + "_action").putExtra("destroy", true).putExtra("redirectToService",true), PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.getService(this, 1, new Intent(this, DNSVpnService.class)
                .setAction(StringUtils.randomString(80) + "_action").putExtra("destroy", true), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentTitle(getString(isRunning ? R.string.active : R.string.paused));
        if(Preferences.getBoolean(this, "show_used_dns",false)){
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().
                bigText("DNS 1: " + dns1 + "\nDNS 2: " + dns2 + "\nDNSV6 1: " + dns1_v6 + "\nDNSV6 2: " + dns2_v6));
            notificationBuilder.setSubText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        }else{
            notificationBuilder.setSubText("");
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().setSummaryText(getString(isRunning ? R.string.notification_running : R.string.notification_paused)));
            notificationBuilder.setContentText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(notificationManager != null && notificationBuilder != null && Preferences.getBoolean(DNSVpnService.this, "setting_show_notification",false)) notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        },10);
    }

    private void initNotification(){
        if (notificationBuilder == null) {
            notificationBuilder = new android.support.v7.app.NotificationCompat.Builder(this);
            notificationBuilder.setSmallIcon(R.drawable.ic_stat_small_icon); //TODO Update Image
            notificationBuilder.setContentTitle(getString(R.string.app_name));
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, PinActivity.class), 0));
            notificationBuilder.setAutoCancel(false);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setUsesChronometer(true);
            notificationBuilder.addAction(new android.support.v4.app.NotificationCompat.Action(R.drawable.ic_stat_pause, getString(R.string.action_pause),null));
            notificationBuilder.addAction(new android.support.v4.app.NotificationCompat.Action(R.drawable.ic_stat_stop, getString(R.string.action_stop),null));
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNotification();
        registerReceiver(stateRequestReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST));
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
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

    public static void updateTiles(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStart.class));
            TileService.requestListeningState(context, new ComponentName(context, TileResume.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePause.class));
            TileService.requestListeningState(context, new ComponentName(context, TileStop.class));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            fixedDNS = intent.hasExtra("fixeddns") ? intent.getBooleanExtra("fixeddns", false) : fixedDNS;
            startedWithTasker = intent.hasExtra("startedWithTasker") ? intent.getBooleanExtra("startedWithTasker", false) : startedWithTasker;
            if(Preferences.getBoolean(this, "auto_pause", false)){
                if(!API.hasUsageStatsPermission(this))Preferences.put(this,"auto_pause",false);
                else autoPauseApps = Preferences.getStringSet(this, "autopause_apps");
            }
            else autoPauseApps = new HashSet<>();
            if(intent.getBooleanExtra("destroy",false)){
                stopped = true;
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
                stopSelf();
            }else if (intent.getBooleanExtra("start_vpn", false)) {
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
                updateDNSServers(intent);
                stopped = false;
                thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
                            initNotification();
                            if(notificationBuilder != null) notificationBuilder.setWhen(System.currentTimeMillis());
                            tunnelInterface = builder.setSession("DnsChanger").addAddress("192.168.255.233", 16).addAddress("fda5:1fed:410b:bbd6:1fad:2abc::1",64).addDnsServer(dns1).addDnsServer(dns2)
                                    .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                            DatagramChannel tunnel = DatagramChannel.open();
                            tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                            protect(tunnel.socket());
                            isRunning = true;
                            broadcastServiceState(true);
                            updateNotification();
                            int counter = 0;
                            try {
                                while (run) {
                                    if(counter >= 4 && autoPauseApps.size() != 0){
                                        counter = 0;
                                        if(autoPauseApps.contains(AppTaskGetter.getMostRecentApp(DNSVpnService.this,1000*1000))){
                                            run = false;
                                            autoPaused = true;
                                            new Thread(autoPausedRestartRunnable).start();
                                        }
                                    }
                                    if(run)Thread.sleep(250);
                                    counter++;
                                }
                            } catch (InterruptedException e2) {

                            }
                        } catch (Exception  e) {
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
            }else if (intent.getBooleanExtra("stop_vpn", false)){
                autoPaused = false;
                if (thread != null) {
                    run = false;
                    thread.interrupt();
                }
            }
            updateTiles(this);
        }
        updateNotification();
        return START_STICKY;
    }
}
