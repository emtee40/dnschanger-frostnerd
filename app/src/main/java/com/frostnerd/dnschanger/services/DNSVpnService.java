package com.frostnerd.dnschanger.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.service.quicksettings.TileService;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.tiles.TilePause;
import com.frostnerd.dnschanger.tiles.TileResume;
import com.frostnerd.dnschanger.tiles.TileStart;
import com.frostnerd.dnschanger.tiles.TileStop;
import com.frostnerd.dnschanger.widgets.BasicWidget;
import com.frostnerd.utils.general.StringUtils;
import com.frostnerd.utils.preferences.Preferences;
import com.frostnerd.utils.stats.AppTaskGetter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
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
    private static final String LOG_TAG = "[DNSVpnService]";
    private static boolean running = false;
    private boolean run = true, isRunning = false, stopped = false;
    private Thread thread;
    private ParcelFileDescriptor tunnelInterface;
    private Builder builder = new Builder();
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 112;
    private Handler handler = new Handler();
    private String dns1,dns2,dns1_v6,dns2_v6;
    private String currentDNS1, currentDNS2, currentDNS1V6, currentDNS2V6;
    private boolean fixedDNS = false, startedWithTasker = false, autoPaused = false;
    private BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[StateRequestReceiver]"}, "Received broadcast", intent);
            broadcastCurrentState(isRunning);
        }
    };
    private Set<String> autoPauseApps;
    private Runnable autoPausedRestartRunnable = new Runnable() {
        @Override
        public void run() {
            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "Started Runnable which'll resume DNSChanger after the autopausing app isn't in the front anymore");
            int counter = 0;
                try {
                    while(!stopped){
                        if(counter >= 4){
                            if(!autoPauseApps.contains(AppTaskGetter.getMostRecentApp(DNSVpnService.this,1000*1000))){
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "No app which autopauses DNS Changer on top anymore. Resuming.");
                                startService(new Intent(DNSVpnService.this, DNSVpnService.class).putExtra("start_vpn",true));
                                break;
                            }
                            counter = 0;
                        }
                        Thread.sleep(250);
                        counter++;
                    }
                } catch (InterruptedException e) {
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "Runnable interrupted");
                    e.printStackTrace();
                }
        }
    };
    private boolean wasCrashShownToUser = false;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Caught uncaught exception");
            LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, e);
            if(!wasCrashShownToUser){
                LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Showing crash to Users");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                ErrorDialogActivity.show(DNSVpnService.this, e);
                wasCrashShownToUser = true;
            }
            stopped = true;
            run = false;
            LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Stopping because of uncaught exception");
            clearVars();
            stopSelf();
        }
    };
    private HashMap<String, Integer> addresses = new HashMap<String, Integer>(){{
        put("172.31.255.253", 30);
        put("172.31.255.1", 28);
        put("192.168.234.55", 24);
        put("192.168.0.1", 24);
    }};

    @Override
    public void onDestroy() {
        LogFactory.writeMessage(this, LOG_TAG, "Destroying");
        clearVars();
        super.onDestroy();
        updateTiles(this);
        LogFactory.writeMessage(this, LOG_TAG, "Destroyed.");
    }

    private void clearVars(){
        LogFactory.writeMessage(this, LOG_TAG, "Clearing Variables");
        stopped = true;
        run = false;
        if (thread != null) thread.interrupt();
        if(stateRequestReceiver != null)LocalBroadcastManager.getInstance(this).unregisterReceiver(stateRequestReceiver);
        thread = null;
        if(notificationManager != null)notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        stateRequestReceiver = null;
        dns1 = dns2 = dns1_v6 = dns2_v6 = currentDNS1 = currentDNS2 = currentDNS1V6 = currentDNS2V6 = null;
        handler = null;
        autoPausedRestartRunnable = null;
        autoPauseApps = null;
        uncaughtExceptionHandler = null;
        running = false;
        LogFactory.writeMessage(this, LOG_TAG, "Variables cleared");
    }

    private void updateNotification() { //Well, this method is a mess.
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Updating notification");
        initNotification();
        if(!Preferences.getBoolean(this, "setting_show_notification",false) && notificationManager != null){
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Notification is disabled");
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }else LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Notification is enabled");
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
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Showing used DNS servers in notification");
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().
                bigText("DNS 1: " + dns1 + "\nDNS 2: " + dns2 + "\nDNSV6 1: " + dns1_v6 + "\nDNSV6 2: " + dns2_v6));
            notificationBuilder.setSubText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        }else{
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Not showing used DNS Servers in notification");
            notificationBuilder.setSubText("");
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().setSummaryText(getString(isRunning ? R.string.notification_running : R.string.notification_paused)));
            notificationBuilder.setContentText(getString(isRunning ? R.string.notification_running : R.string.notification_paused));
        }
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Posting Notification in 10ms");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(notificationManager != null && notificationBuilder != null && Preferences.getBoolean(DNSVpnService.this, "setting_show_notification",false)){
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Updating notification");
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }else{
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Not updating notification (Builder might have become null or notification was disabled)");
                }
            }
        },10);
    }

    private void initNotification(){
        if (notificationBuilder == null) {
            LogFactory.writeMessage(this,new String[]{LOG_TAG, "[NOTIFICATION]"} , "Initiating Notification");
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
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Notification created (Not yet posted)");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Created Service");
        initNotification();
        LocalBroadcastManager.getInstance(this).registerReceiver(stateRequestReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST));
    }

    public static void startWithSetDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6){
        Intent i;
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Starting DNSVPNService with fixed DNS",
                i = new Intent(context, DNSVpnService.class).putExtra("fixeddns",true).
                        putExtra("dns1", dns1).putExtra("dns2", dns2).putExtra("dns1-v6", dns1v6).putExtra("dns2-v6", dns2v6));
        context.startService(i);
    }

    private void updateDNSServers(Intent intent){
        LogFactory.writeMessage(this, LOG_TAG, "Updating DNS Servers");
        if(fixedDNS){
            LogFactory.writeMessage(this, LOG_TAG, "DNSVPNService is using fixed DNS servers (Not those from settings)");
            LogFactory.writeMessage(this, LOG_TAG, "Current DNS Servers; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
            if(intent == null)return;
            if(intent.hasExtra("dns1"))dns1 = intent.getStringExtra("dns1");
            if(intent.hasExtra("dns2"))dns2 = intent.getStringExtra("dns2");
            if(intent.hasExtra("dns1-v6"))dns1_v6 = intent.getStringExtra("dns1-v6");
            if(intent.hasExtra("dns2-v6"))dns2_v6 = intent.getStringExtra("dns2-v6");
            LogFactory.writeMessage(this, LOG_TAG, "DNS Servers set to; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Not using fixed DNS. Fetching DNS from settings");
            LogFactory.writeMessage(this, LOG_TAG, "Current DNS Servers; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
            dns1 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.8.8");
            dns2 = Preferences.getString(DNSVpnService.this, "dns2", "8.8.4.4");
            dns1_v6 = Preferences.getString(DNSVpnService.this, "dns1-v6", "2001:4860:4860::8888");
            dns2_v6 = Preferences.getString(DNSVpnService.this, "dns2-v6", "2001:4860:4860::8844");
            LogFactory.writeMessage(this, LOG_TAG, "DNS Servers set to; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
        }
    }

    public static void updateTiles(Context context){
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Trying to update Tiles");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStart.class));
            TileService.requestListeningState(context, new ComponentName(context, TileResume.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePause.class));
            TileService.requestListeningState(context, new ComponentName(context, TileStop.class));
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Tiles updated");
        }else LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Not updating Tiles (Version is below Android N)");
    }

    private void broadcastCurrentState(boolean vpnRunning){
        Intent i;
        LogFactory.writeMessage(this, LOG_TAG, "Broadcasting current Service state",
                i = new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).
                        putExtra("dns1", currentDNS1).putExtra("dns2", currentDNS2).putExtra("dns1v6", currentDNS1V6).putExtra("dns2v6", currentDNS2V6).
                        putExtra("vpn_running", isRunning).putExtra("started_with_tasker", startedWithTasker).putExtra("fixedDNS", fixedDNS));
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        LogFactory.writeMessage(this, LOG_TAG, "Broadcasted service state.");
    }

    // Hello potential Source-Code lurker!
    // I've stumbled upon many os-based problems developing this application. For example some devices don't accept the IP used (172.31.255.253/30 is preferred)
    // and mark it as "bad address". Other devices have a problem with the underlying VPN structure which prevents it from working on the first try.
    // Other devices don't even reach the while-loop (by design) and thus I have to catch every exception occurring.
    // Overall this lead to a messy thread and a lot of flawed design, but there is no other way to do it which makes it work
    // on all devices.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Got StartCommand", intent);
        if(intent!=null){
            running = !intent.getBooleanExtra("destroy", false);
            API.updateAllWidgets(this, BasicWidget.class);
            fixedDNS = intent.hasExtra("fixeddns") ? intent.getBooleanExtra("fixeddns", false) : fixedDNS;
            startedWithTasker = intent.hasExtra("startedWithTasker") ? intent.getBooleanExtra("startedWithTasker", false) : startedWithTasker;
            if(Preferences.getBoolean(this, "auto_pause", false)){
                if(!API.hasUsageStatsPermission(this))Preferences.put(this,"auto_pause",false);
                else autoPauseApps = Preferences.getStringSet(this, "autopause_apps");
            }
            else autoPauseApps = new HashSet<>();
            if(intent.getBooleanExtra("destroy",false)){
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Got destroy. Destroying DNSVPNService");
                stopped = true;
                if (thread != null) {
                    LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Interrupting Thread");
                    run = false;
                    thread.interrupt();
                }
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Stopping self");
                stopSelf();
            }else if (intent.getBooleanExtra("start_vpn", false)) {
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Starting VPN");
                if (thread != null) {
                    LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "VPNThread already running. Interrupting");
                    run = false;
                    thread.interrupt();
                }
                updateDNSServers(intent);
                stopped = false;
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Creating Thread");
                thread = new Thread(new Runnable() {
                    private int addressIndex = 0;

                    @Override
                    public void run() {
                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Starting Thread (run)");
                        DatagramChannel tunnel = null;
                        DatagramSocket tunnelSocket = null;
                        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
                        initNotification();
                        if(notificationBuilder != null) notificationBuilder.setWhen(System.currentTimeMillis());
                        currentDNS1 = dns1;
                        currentDNS2 = dns2;
                        currentDNS1V6 = dns1_v6;
                        currentDNS2V6 = dns2_v6;
                        try {
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Trying " + addresses.size() + " different addresses before passing any thrown exception to the upper layer");
                            for(String address: addresses.keySet()){
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Trying address '" + address + "'");
                                try{
                                    addressIndex++;
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Creating Tunnel interface");
                                    tunnelInterface = builder.setSession("DnsChanger").addAddress(address, addresses.get(address)).addAddress(API.randomLocalIPv6Address(),48).addDnsServer(dns1).addDnsServer(dns2)
                                            .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Tunnel interface created and established.");
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Opening DatagramChannel");
                                    tunnel = DatagramChannel.open();
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "DatagramChannel opened");
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Connecting to 127.0.0.1:8087");
                                    tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Connected");
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Trying to protect tunnel");
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Tunnel protected: " + protect(tunnelSocket=tunnel.socket()));
                                    isRunning = true;
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Sending broadcast with current state");
                                    broadcastCurrentState(true);
                                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Broadcast sent");
                                    updateNotification();
                                    int counter = 0;
                                    try {
                                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "VPN Thread going into while loop");
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
                                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "VPN Thread reached end of while loop. Run: " + run);
                                    } catch (InterruptedException e2) {
                                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Thread was interrupted");
                                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Interruption stacktrace: " + LogFactory.stacktraceToString(e2).replace("\n"," <<-->>"));
                                        break;
                                    }
                                }catch(Exception e){
                                    LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]"}, e);
                                    if(addressIndex >= addresses.size())throw e;
                                    else LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]"},
                                            "Not throwing exception. Tries: " + addressIndex + ", addresses: " + addresses.size());
                                }
                            }
                        } catch (Exception  e) {
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "VPN Thread had an exception");
                            LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG,LogFactory.Tag.ERROR.toString()}, e);
                            e.printStackTrace();
                            ErrorDialogActivity.show(DNSVpnService.this, e);
                        } finally {
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "VPN Thread is in finally block");
                            currentDNS1 = currentDNS2 = currentDNS1V6 = currentDNS2V6 = null;
                            isRunning = false;
                            updateNotification();
                            if (tunnelInterface != null) try {
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Closing tunnel interface");
                                tunnelInterface.close();
                            } catch (IOException e) {
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Exception received whilst closing tunnel: " + e.getMessage());
                                e.printStackTrace();
                            }
                            if(tunnel != null)try{
                                tunnel.close();
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            if(tunnelSocket != null)try{
                                tunnelSocket.close();
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Sending broadcast with current state");
                            broadcastCurrentState(false);
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]"}, "Broadcast sent");
                            thread = null;
                        }
                    }
                });
                run = true;
                thread.start();
            }else if (intent.getBooleanExtra("stop_vpn", false)){
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Stopping VPN");
                autoPaused = false;
                if (thread != null) {
                    LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Interrupting thread");
                    run = false;
                    thread.interrupt();
                }
            }
            updateTiles(this);
        }else LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]", LogFactory.Tag.ERROR.toString()}, "Intent given is null. This isn't normal behavior");
        updateNotification();
        return START_STICKY;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LogFactory.writeMessage(this, LOG_TAG, "Got onLowMemory");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LogFactory.writeMessage(this, LOG_TAG, "Memory trimmed. Level: " + level);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        LogFactory.writeMessage(this, LOG_TAG, "Task is being removed. ", rootIntent);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return intent.getBooleanExtra("binder",false) ? new ServiceBinder() : null;
    }

    public static Intent getBinderIntent(Context context){
        return new Intent(context, DNSVpnService.class).putExtra("binder",true);
    }

    public String getCurrentDNS1() {
        return currentDNS1;
    }

    public String getCurrentDNS2() {
        return currentDNS2;
    }

    public String getCurrentDNS1V6() {
        return currentDNS1V6;
    }

    public String getCurrentDNS2V6() {
        return currentDNS2V6;
    }

    public boolean startedFromShortcut(){
        return fixedDNS && !startedWithTasker;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public class ServiceBinder extends Binder{
        public DNSVpnService getService(){
            return DNSVpnService.this;
        }
    }

    public static boolean isServiceRunning(){
        return running;
    }
}
