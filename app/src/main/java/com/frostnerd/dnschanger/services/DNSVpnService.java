package com.frostnerd.dnschanger.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArraySet;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.widgets.BasicWidget;
import com.frostnerd.utils.general.IntentUtil;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.general.WidgetUtil;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.utils.preferences.Preferences;
import com.frostnerd.utils.stats.AppTaskGetter;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

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
    private static boolean serviceRunning = false, threadRunning = false;
    private Thread vpnThread;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private final int NOTIFICATION_ID = 112;
    private String dns1,dns2,dns1_v6,dns2_v6, stopReason, currentDNS1, currentDNS2, currentDNS1V6, currentDNS2V6;
    private Vector<Runnable> afterThreadStop = new Vector<>();

    private boolean fixedDNS = false, startedWithTasker = false, autoPaused = false, runThread = true, variablesCleared = false;
    private BroadcastReceiver stateRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[StateRequestReceiver]"}, "Received broadcast", intent);
            broadcastCurrentState(threadRunning);
        }
    };
    private Set<String> autoPauseApps;
    private Runnable autoPausedRestartRunnable = new Runnable() {
        @Override
        public void run() {
            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "Started Runnable which'll resume DNSChanger after the autopausing app isn't in the front anymore");
            int counter = 0;
            try {
                while(serviceRunning){
                    if(counter >= 4){
                        if(!autoPauseApps.contains(AppTaskGetter.getMostRecentApp(DNSVpnService.this,1000*1000))){
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "No app which autopauses DNS Changer on top anymore. Resuming.");
                            startService(new Intent(DNSVpnService.this, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),true));
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
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Caught uncaught exception");
            LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, e);
            LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Showing crash to Users");
            ErrorDialogActivity.show(DNSVpnService.this, e);
            LogFactory.writeMessage(DNSVpnService.this, LOG_TAG, "Stopping because of uncaught exception");
            stopReason = getString(R.string.reason_stop_exception);
            stopService();
        }
    };
    private Map<String, Integer> addresses = new ConcurrentHashMap<String, Integer>(){{
        put("172.31.255.253", 30);
        put("192.168.0.1", 24);
        put("192.168.234.55", 24);
        put("172.31.255.1", 28);
    }};

    private synchronized void clearVars(boolean stopSelf){
        if(variablesCleared)return;
        variablesCleared = true;
        LogFactory.writeMessage(this, LOG_TAG, "Clearing Variables");
        if(stopReason != null && notificationManager != null && Preferences.getBoolean(this, "notification_on_stop", false)){
            String reasonText = getString(R.string.notification_reason_stopped).replace("[reason]", stopReason);
            notificationManager.notify(NOTIFICATION_ID+1, new NotificationCompat.Builder(this).setAutoCancel(true)
                    .setOngoing(false).setContentText(reasonText).setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText(reasonText))
                    .setSmallIcon(R.drawable.ic_stat_small_icon).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, PinActivity.class), 0))
                    .build());
        }
        serviceRunning = false;
        stopThread();
        if(stateRequestReceiver != null)LocalBroadcastManager.getInstance(this).unregisterReceiver(stateRequestReceiver);
        stopForeground(true);
        notificationManager = null;
        notificationBuilder = null;
        stateRequestReceiver = null;
        dns1 = dns2 = dns1_v6 = dns2_v6 = currentDNS1 = currentDNS2 = currentDNS1V6 = currentDNS2V6 = null;
        autoPausedRestartRunnable = null;
        autoPauseApps = null;
        uncaughtExceptionHandler = null;
        threadRunning = false;
        afterThreadStop.clear();
        afterThreadStop = null;
        LogFactory.writeMessage(this, LOG_TAG, "Variables cleared");
        if(stopSelf)stopSelf();
    }

    private void updateNotification() { //Well, this method is a mess.
        if(!serviceRunning)return;
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Updating notification");
        initNotification();
        if(!Preferences.getBoolean(this, "setting_show_notification",false)){
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Notification is disabled");
            stopForeground(true);
            return;
        }else LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Notification is enabled");
        boolean pinProtected = Preferences.getBoolean(this, "pin_notification",false);
        android.support.v4.app.NotificationCompat.Action a1 = notificationBuilder.mActions.get(0);
        a1.icon = threadRunning ? R.drawable.ic_stat_pause : R.drawable.ic_stat_resume;
        a1.title = getString(threadRunning ? R.string.action_pause : R.string.action_resume);
        a1.actionIntent = pinProtected ? PendingIntent.getActivity(this,0,new Intent(this, PinActivity.class).setAction(new Random().nextInt(50) + "_action").
                putExtra(threadRunning ? "stop_vpn" : "start_vpn", true).putExtra("redirectToService",true), PendingIntent.FLAG_UPDATE_CURRENT) :
                PendingIntent.getService(this, 0, new Intent(this, DNSVpnService.class).setAction(new Random().nextInt(50) + "_action").putExtra(threadRunning ? VPNServiceArgument.COMMAND_STOP_VPN.getArgument() : VPNServiceArgument.COMMAND_START_VPN.getArgument(), true), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.mActions.get(1).actionIntent = pinProtected ? PendingIntent.getActivity(this,0,new Intent(this, PinActivity.class).
                setAction(new Random().nextInt(50) + "_action").putExtra("destroy", true).putExtra("redirectToService",true), PendingIntent.FLAG_UPDATE_CURRENT) : PendingIntent.getService(this, 1, new Intent(this, DNSVpnService.class)
                .setAction(StringUtil.randomString(80) + "_action").putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(), true), PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentTitle(getString(threadRunning ? R.string.active : R.string.paused));
        if(Preferences.getBoolean(this, "show_used_dns",false)){
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Showing used DNS servers in notification");
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText("DNS 1: " + getCurrentDNS1() + "\nDNS 2: " + getCurrentDNS2() + "\nDNSV6 1: " + getCurrentDNS1V6() + "\nDNSV6 2: " + getCurrentDNS2V6()));
            notificationBuilder.setSubText(getString(threadRunning ? R.string.notification_running : R.string.notification_paused));
        }else{
            LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Not showing used DNS Servers in notification");
            notificationBuilder.setSubText("");
            notificationBuilder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().setSummaryText(getString(threadRunning ? R.string.notification_running : R.string.notification_paused)));
            notificationBuilder.setContentText(getString(threadRunning ? R.string.notification_running : R.string.notification_paused));
        }
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Posting Notification in 10ms");
        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[NOTIFICATION]"}, "Updating notification");
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
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

    public static synchronized void startWithSetDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6){
        Intent i;
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Starting DNSVPNService with fixed DNS",
                i = new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(),true).
                        putExtra(VPNServiceArgument.ARGUMENT_DNS1.getArgument(), dns1).putExtra(VPNServiceArgument.ARGUMENT_DNS2.getArgument(), dns2).
                        putExtra(VPNServiceArgument.ARGUMENT_DNS1V6.getArgument(), dns1v6).putExtra(VPNServiceArgument.ARGUMENT_DNS2V6.getArgument(), dns2v6));
        context.startService(i);
    }

    private void updateDNSServers(Intent intent){
        LogFactory.writeMessage(this, LOG_TAG, "Updating DNS Servers");
        if(fixedDNS){
            LogFactory.writeMessage(this, LOG_TAG, "DNSVPNService is using fixed DNS servers (Not those from settings)");
            LogFactory.writeMessage(this, LOG_TAG, "Current DNS Servers; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
            if(intent != null){
                if(intent.hasExtra(VPNServiceArgument.ARGUMENT_DNS1.getArgument()))dns1 = intent.getStringExtra(VPNServiceArgument.ARGUMENT_DNS1.getArgument());
                if(intent.hasExtra(VPNServiceArgument.ARGUMENT_DNS2.getArgument()))dns2 = intent.getStringExtra(VPNServiceArgument.ARGUMENT_DNS2.getArgument());
                if(intent.hasExtra(VPNServiceArgument.ARGUMENT_DNS1V6.getArgument()))dns1_v6 = intent.getStringExtra(VPNServiceArgument.ARGUMENT_DNS1V6.getArgument());
                if(intent.hasExtra(VPNServiceArgument.ARGUMENT_DNS2V6.getArgument()))dns2_v6 = intent.getStringExtra(VPNServiceArgument.ARGUMENT_DNS2V6.getArgument());
                LogFactory.writeMessage(this, LOG_TAG, "DNS Servers set to; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
            }
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Not using fixed DNS. Fetching DNS from settings");
            LogFactory.writeMessage(this, LOG_TAG, "Current DNS Servers; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
            dns1 = Preferences.getString(DNSVpnService.this, "dns1", "8.8.8.8");
            dns2 = Preferences.getString(DNSVpnService.this, "dns2", "8.8.4.4");
            dns1_v6 = Preferences.getString(DNSVpnService.this, "dns1-v6", "2001:4860:4860::8888");
            dns2_v6 = Preferences.getString(DNSVpnService.this, "dns2-v6", "2001:4860:4860::8844");
            LogFactory.writeMessage(this, LOG_TAG, "DNS Servers set to; DNS1: " + dns1 + ", DNS2: " + dns2 + ", DNS1V6:" + dns1_v6 + ", DNS2V6: " + dns2_v6);
        }
        checkDNSValid(true);
        checkDNSValid(false);
    }

    private void checkDNSValid(boolean fromPreferences){
        if(dns1 == null || dns1.equals("") || !NetworkUtil.isIPv4(dns1))
            dns1 = fromPreferences ? Preferences.getString(this,"dns1","8.8.8.8") : "8.8.8.8";
        if(dns2 == null || dns2.equals("") || !NetworkUtil.isIPv4(dns2))
            dns2 = fromPreferences ? Preferences.getString(this,"dns2","8.8.4.4") : "8.8.4.4";
        if(dns1_v6 == null || dns1_v6.equals("") || !NetworkUtil.isIP(dns1_v6,true))
            dns1_v6 = fromPreferences ? Preferences.getString(this,"dns1-v6","2001:4860:4860::8888") : "2001:4860:4860::8888";
        if(dns2_v6 == null || dns2_v6.equals("") || !NetworkUtil.isIP(dns2_v6, true))
            dns2_v6 = fromPreferences ? Preferences.getString(this,"dns2-v6","2001:4860:4860::8844") : "2001:4860:4860::8844";
    }

    private void broadcastCurrentState(boolean vpnRunning){
        Intent i;
        LogFactory.writeMessage(this, LOG_TAG, "Broadcasting current Service state",
                i = new Intent(API.BROADCAST_SERVICE_STATUS_CHANGE).
                        putExtra("dns1", currentDNS1).putExtra("dns2", currentDNS2).putExtra("dns1v6", currentDNS1V6).putExtra("dns2v6", currentDNS2V6).
                        putExtra("vpn_running", threadRunning).putExtra("started_with_tasker", startedWithTasker).putExtra("fixedDNS", fixedDNS));
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
        intent = intent == null ? intent : restoreSettings(intent);
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Got StartCommand", intent);
        serviceRunning = intent == null || !intent.getBooleanExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(), false);
        if(intent!=null){
            WidgetUtil.updateAllWidgets(this, BasicWidget.class);
            fixedDNS = intent.hasExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument()) ? intent.getBooleanExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(), false) : fixedDNS;
            updateDNSServers(intent);
            startedWithTasker = intent.hasExtra(VPNServiceArgument.FLAG_STARTED_WITH_TASKER.getArgument()) ? intent.getBooleanExtra(VPNServiceArgument.FLAG_STARTED_WITH_TASKER.getArgument(), false) : startedWithTasker;
            if(Preferences.getBoolean(this, "auto_pause", false)){
                if(!PermissionsUtil.hasUsageStatsPermission(this)){
                    Preferences.put(this,"auto_pause",false);
                    if(autoPauseApps != null)autoPauseApps.clear();
                }
                else autoPauseApps = Preferences.getStringSet(this, "autopause_apps");
            }else autoPauseApps = new HashSet<>();
            if(IntentUtil.checkExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(),intent)){
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Got destroy. Destroying DNSVPNService");
                if(intent.hasExtra(VPNServiceArgument.ARGUMENT_STOP_REASON.getArgument()))stopReason = intent.getStringExtra(VPNServiceArgument.ARGUMENT_STOP_REASON.getArgument());
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Stopping self");
                stopService();
            }else if (IntentUtil.checkExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),intent)) {
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Starting VPN");
                if(threadRunning){
                    if(!IntentUtil.checkExtra(VPNServiceArgument.FLAG_DONT_START_IF_RUNNING.getArgument(), intent)){
                        afterThreadStop.add(new Runnable() {
                            @Override
                            public void run() {
                                vpnThread = createThread();
                                vpnThread.start();
                            }
                        });
                        stopThread();
                    }
                }else {
                    vpnThread = createThread();
                    vpnThread.start();
                }
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Creating Thread");
            }else if (IntentUtil.checkExtra(VPNServiceArgument.COMMAND_STOP_VPN.getArgument(),intent)){
                LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]"}, "Stopping VPN");
                stopThread();
            }
            API.updateTiles(this);
        }else LogFactory.writeMessage(this, new String[]{LOG_TAG, "[ONSTARTCOMMAND]", LogFactory.Tag.ERROR.toString()}, "Intent given is null. This isn't normal behavior");
        updateNotification();
        return START_STICKY;
    }

    public void stopService(){
        clearVars(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Created Service");
        initNotification();
        LocalBroadcastManager.getInstance(this).registerReceiver(stateRequestReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATE_REQUEST));
    }

    @Override
    public void onDestroy() {
        LogFactory.writeMessage(this, LOG_TAG, "Destroying");
        clearVars(false);
        super.onDestroy();
        API.updateTiles(this);
        LogFactory.writeMessage(this, LOG_TAG, "Destroyed.");
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
        backupSettings();
        clearVars(false);
        super.onTaskRemoved(rootIntent);
    }

    //As the service could be removed from RAM when the main activity is destroyed this function memorizes what settings where used if that happens.
    private void backupSettings(){
        LogFactory.writeMessage(this, LOG_TAG, "Backing up settings to resume with them as soon as the service restarts");
        Set<String> settings = new ArraySet<>();
        settings.add(VPNServiceArgument.ARGUMENT_DNS1 + ";;" + dns1);
        settings.add(VPNServiceArgument.ARGUMENT_DNS2 + ";;" +  dns2);
        settings.add(VPNServiceArgument.ARGUMENT_DNS1V6 + ";;" + dns1_v6);
        settings.add(VPNServiceArgument.ARGUMENT_DNS2V6 + ";;" + dns2_v6);
        settings.add(VPNServiceArgument.FLAG_STARTED_WITH_TASKER + ";;" + startedWithTasker);
        settings.add(VPNServiceArgument.FLAG_FIXED_DNS + ";;" + fixedDNS);
        settings.add(VPNServiceArgument.COMMAND_START_VPN + ";;" + threadRunning);
        settings.add("Running;;" + threadRunning);
        Preferences.put(this, "settings_backup", settings);
        Preferences.put(this, "settings_backuped", true);
        LogFactory.writeMessage(this, LOG_TAG, "Backup finished");
    }

    private Intent restoreSettings(Intent i){
        LogFactory.writeMessage(this, LOG_TAG, "Restoring settings if needed...");
        if(Preferences.getBoolean(this, "settings_backuped",false)){
            LogFactory.writeMessage(this, LOG_TAG, "Settings of previous service state were saved");
            Preferences.put(this, "settings_backuped", false);
            Set<String> settings = Preferences.getStringSet(this, "settings_backup");
            String key, value;
            String splt[];
            for(VPNServiceArgument argument: VPNServiceArgument.values()){
                for(String s: settings){
                    splt = s.split(";;");
                    key = splt[0];
                    value = splt[1];
                    if(key.equalsIgnoreCase(argument.getArgument())){
                        LogFactory.writeMessage(this, LOG_TAG, "Putting " + key + ": '" + value + "'");
                        IntentUtil.putExtra(key, i, Preferences.getType(value).parseTo(value));
                    }
                }
            }
            Preferences.put(this, "settings_backup", null);
        }else LogFactory.writeMessage(this, LOG_TAG, "No setting were previously saved.");
        return i;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return (intent.getBooleanExtra(VPNServiceArgument.FLAG_GET_BINDER.getArgument(),false) && serviceRunning) ? new ServiceBinder() : null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    private void stopThread(){
        LogFactory.writeMessage(this, LOG_TAG, "Trying to stop thread.");
        if (vpnThread != null) {
            LogFactory.writeMessage(this, LOG_TAG, "VPNThread already running. Interrupting");
            runThread = false;
            vpnThread.interrupt();
        }else{
            threadRunning = false;
            LogFactory.writeMessage(this, LOG_TAG, "VPNThread not running. No need to interrupt.");
        }
    }

    private Thread createThread(){
        return new Thread(new Runnable() {
            private final String ID = "[" + StringUtil.randomString(20) + "]";
            private int addressIndex = 0;
            private ParcelFileDescriptor tunnelInterface = null;
            private Builder builder;
            private DatagramChannel tunnel = null;
            private DatagramSocket tunnelSocket = null;

            @Override
            public void run() {
                threadRunning = true;
                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Starting Thread (run)");
                runThread = true;
                Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
                if(notificationBuilder != null) notificationBuilder.setWhen(System.currentTimeMillis());
                try {
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Trying " + addresses.size() + " different addresses before passing any thrown exception to the upper layer");
                    for(String address: addresses.keySet()){
                        LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Trying address '" + address + "'");
                        try{
                            addressIndex++;
                            builder = new Builder();
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Creating Tunnel interface");
                            tunnelInterface = builder.setSession("DnsChanger" + StringUtil.randomString(50)).addAddress(address, addresses.get(address)).addAddress(NetworkUtil.randomLocalIPv6Address(),48).addDnsServer(dns1).addDnsServer(dns2)
                                    .addDnsServer(dns1_v6).addDnsServer(dns2_v6).establish();
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel interface created and established.");
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Opening DatagramChannel");
                            tunnel = DatagramChannel.open();
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "DatagramChannel opened");
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Connecting to 127.0.0.1:8087");
                            tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Connected");
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Trying to protect tunnel");
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Tunnel protected: " + protect(tunnelSocket=tunnel.socket()));
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Sending broadcast with current state");
                            broadcastCurrentState(true);
                            LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Broadcast sent");
                            currentDNS1 = dns1;
                            currentDNS2 = dns2;
                            currentDNS1V6 = dns1_v6;
                            currentDNS2V6 = dns2_v6;
                            updateNotification();
                            int counter = 0;
                            try {
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread going into while loop");
                                if(autoPauseApps != null && autoPauseApps.size() != 0){
                                    while (runThread) {
                                        if(counter >= 4 && autoPauseApps.size() != 0){
                                            counter = 0;
                                            if(autoPauseApps.contains(AppTaskGetter.getMostRecentApp(DNSVpnService.this,1000*1000))){
                                                runThread = false;
                                                autoPaused = true;
                                                new Thread(autoPausedRestartRunnable).start();
                                            }
                                        }
                                        Thread.sleep(250);
                                        counter++;
                                    }
                                }else{
                                    while(runThread){
                                        Thread.sleep(250);
                                    }
                                }
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread reached end of while loop. Run: " + runThread);
                                break;
                            } catch (InterruptedException e2) {
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Thread was interrupted");
                                LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Interruption stacktrace: " + LogFactory.stacktraceToString(e2).replace("\n"," <<-->>"));
                                break;
                            }
                        }catch(Exception e){
                            LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]", ID}, e);
                            if(addressIndex >= addresses.size())throw e;
                            else LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", "[ADDRESS-RETRY]", ID},
                                    "Not throwing exception. Tries: " + addressIndex + ", addresses: " + addresses.size());
                            if(!serviceRunning)break;
                        }finally{
                            clearVars();
                        }
                    }
                } catch (Exception  e) {
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread had an exception");
                    LogFactory.writeStackTrace(DNSVpnService.this, new String[]{LOG_TAG,LogFactory.Tag.ERROR.toString()}, e);
                    e.printStackTrace();
                    ErrorDialogActivity.show(DNSVpnService.this, e);
                } finally {
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "VPN Thread is in finally block");
                    threadRunning = false;
                    currentDNS1 = currentDNS2 = currentDNS1V6 = currentDNS2V6 = null;
                    clearVars();
                    updateNotification();
                    broadcastCurrentState(false);
                    if(afterThreadStop != null && serviceRunning){
                        for(Runnable r: afterThreadStop)r.run();
                        afterThreadStop.clear();
                    }
                    vpnThread = null;
                    LogFactory.writeMessage(DNSVpnService.this, new String[]{LOG_TAG, "[VPNTHREAD]", ID}, "Done with finally block");
                }
            }

            private synchronized void clearVars(){
                if(tunnelSocket != null && !tunnelSocket.isClosed()){
                    tunnelSocket.disconnect();
                    tunnelSocket.close();
                }
                if(tunnel != null && tunnel.isConnected()) try {
                    tunnel.disconnect();
                    tunnel.close();
                } catch (IOException | IllegalStateException e) {
                    e.printStackTrace();
                }
                if(tunnelInterface != null){
                    try {
                        tunnelInterface.close();
                    } catch (IOException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
                builder = null;
                tunnel = null;
                tunnelInterface = null;
                tunnelSocket = null;
            }
        });
    }

    public static Intent getDestroyIntent(Context context){
        return getDestroyIntent(context, null);
    }

    public static Intent getDestroyIntent(Context context, String reason){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(), true).
                putExtra(VPNServiceArgument.ARGUMENT_STOP_REASON.getArgument(), reason).
                putExtra(VPNServiceArgument.ARGUMENT_CALLER_TRACE.getArgument(), LogFactory.stacktraceToString(new Throwable()));
    }

    public static Intent getStartVPNIntent(Context context){
        return getStartVPNIntent(context, false);
    }

    public static Intent getStartVPNIntent(Context context, boolean startedWithTasker){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),true).putExtra(VPNServiceArgument.FLAG_STARTED_WITH_TASKER.getArgument(), startedWithTasker).
                putExtra(VPNServiceArgument.ARGUMENT_CALLER_TRACE.getArgument(), LogFactory.stacktraceToString(new Throwable(),true));
    }

    public static Intent getStopVPNIntent(Context context){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_STOP_VPN.getArgument(),true).
                putExtra(VPNServiceArgument.ARGUMENT_CALLER_TRACE.getArgument(), LogFactory.stacktraceToString(new Throwable(),true));
    }

    public static Intent getStartVPNIntent(Context context, String dns1, String dns2, String dns1v6, String dns2v6, boolean startedWithTasker, boolean fixedDNS){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),true).
                putExtra(VPNServiceArgument.ARGUMENT_DNS1.getArgument(), dns1).putExtra(VPNServiceArgument.ARGUMENT_DNS2.getArgument(), dns2).
                putExtra(VPNServiceArgument.ARGUMENT_DNS1V6.getArgument(), dns1v6).putExtra(VPNServiceArgument.ARGUMENT_DNS2V6.getArgument(), dns2v6).
                putExtra(VPNServiceArgument.FLAG_STARTED_WITH_TASKER.getArgument(), startedWithTasker). putExtra(VPNServiceArgument.FLAG_FIXED_DNS.getArgument(), fixedDNS).
                putExtra(VPNServiceArgument.ARGUMENT_CALLER_TRACE.getArgument(), LogFactory.stacktraceToString(new Throwable(),true));
    }

    public static Intent getStartVPNIntent(Context context, String dns1, String dns2, String dns1v6, String dns2v6, boolean startedWithTasker){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),true).
                putExtra(VPNServiceArgument.ARGUMENT_DNS1.getArgument(), dns1).putExtra(VPNServiceArgument.ARGUMENT_DNS2.getArgument(), dns2).
                putExtra(VPNServiceArgument.ARGUMENT_DNS1V6.getArgument(), dns1v6).putExtra(VPNServiceArgument.ARGUMENT_DNS2V6.getArgument(), dns2v6).
                putExtra(VPNServiceArgument.FLAG_STARTED_WITH_TASKER.getArgument(), startedWithTasker).
                putExtra(VPNServiceArgument.ARGUMENT_CALLER_TRACE.getArgument(), LogFactory.stacktraceToString(new Throwable(),true));
    }

    public static Intent getBinderIntent(Context context){
        return new Intent(context, DNSVpnService.class).putExtra(VPNServiceArgument.FLAG_GET_BINDER.getArgument(),true);
    }

    public String getCurrentDNS1() {
        return !threadRunning || currentDNS1 == null ? dns1 : currentDNS1;
    }

    public String getCurrentDNS2() {
        return !threadRunning || currentDNS2 == null ? dns2 : currentDNS2;
    }

    public String getCurrentDNS1V6() {
        return !threadRunning || currentDNS1V6 == null? dns1_v6 : currentDNS1V6;
    }

    public String getCurrentDNS2V6() {
        return !threadRunning || currentDNS2V6 == null ? dns2_v6 : currentDNS2V6;
    }
    public boolean startedFromShortcut(){
        return fixedDNS && !startedWithTasker;
    }

    public static boolean isDNSThreadRunning(){
        return isServiceRunning() && threadRunning;
    }

    public class ServiceBinder extends Binder{
        public DNSVpnService getService(){
            return DNSVpnService.this;
        }
    }

    public static boolean isServiceRunning(){
        return serviceRunning;
    }

}
