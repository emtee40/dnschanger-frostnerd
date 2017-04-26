package com.frostnerd.dnschanger.services;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.IntDef;
import android.support.v7.app.NotificationCompat;

import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.utils.stats.AppTaskGetter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class TestVPNService extends VpnService{
    private Builder builder = new Builder();
    private ParcelFileDescriptor mInterface;
    private boolean shouldRun = true;
    private  final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };
    private DatagramChannel tunnel;
    private Thread mThread;

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
            LogFactory.writeMessage(TestVPNService.this, new String[]{LOG_TAG, "[StateRequestReceiver]"}, "Received broadcast", intent);
            broadcastCurrentState(threadRunning);
        }
    };
    private Set<String> autoPauseApps;
    private Runnable autoPausedRestartRunnable = new Runnable() {
        @Override
        public void run() {
            LogFactory.writeMessage(TestVPNService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "Started Runnable which'll resume DNSChanger after the autopausing app isn't in the front anymore");
            int counter = 0;
            try {
                while(serviceRunning){
                    if(counter >= 4){
                        if(!autoPauseApps.contains(AppTaskGetter.getMostRecentApp(TestVPNService.this,1000*1000))){
                            LogFactory.writeMessage(TestVPNService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "No app which autopauses DNS Changer on top anymore. Resuming.");
                            startService(new Intent(TestVPNService.this, DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(),true));
                            break;
                        }
                        counter = 0;
                    }
                    Thread.sleep(250);
                    counter++;
                }
            } catch (InterruptedException e) {
                LogFactory.writeMessage(TestVPNService.this, new String[]{LOG_TAG,"[AutoPausedRestartRunnable]"}, "Runnable interrupted");
                e.printStackTrace();
            }
        }
    };
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LogFactory.writeMessage(TestVPNService.this, LOG_TAG, "Caught uncaught exception");
            LogFactory.writeStackTrace(TestVPNService.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, e);
            LogFactory.writeMessage(TestVPNService.this, LOG_TAG, "Showing crash to Users");
            ErrorDialogActivity.show(TestVPNService.this, e);
            LogFactory.writeMessage(TestVPNService.this, LOG_TAG, "Stopping because of uncaught exception");
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

    private void registerBroadcast(){
        IntentFilter action = new IntentFilter("STOP_ASD");
        registerReceiver(stopServiceReceiver, action);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        if(intent.getBooleanExtra("asddsa",false)){
            registerBroadcast();
            mThread = createThread();
            mThread.start();
        }
        return START_STICKY;
    }

    private void stopThread(){

    }

    private void restoreSettings(){

    }

    private void backupSettings(){

    }

    public void stopService(){

    }

    private void broadcastCurrentState(boolean run){

    }

    private void checkDNSValid(boolean pref){

    }

    private void updateDNSServers(Intent intent){

    }

    private void startWithSetDNS(final Context context, final String dns1, final String dns2, final String dns1v6, final String dns2v6){

    }

    private void initNotification(){

    }

    private void updateNotification() {

    }

    private synchronized void clearVars(boolean stopSelf){

    }

    private Thread createThread(){
        return new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mInterface =  builder.setSession("DNSChanger").addAddress("192.168.0.1",24).addDnsServer("8.8.8.8").addDnsServer("8.8.4.4").establish();
                    tunnel = DatagramChannel.open();
                    tunnel.connect(new InetSocketAddress("127.0.0.1", 8087));
                    protect(tunnel.socket());
                    while(shouldRun){
                        Thread.sleep(100);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "DNSChanger");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return intent.hasExtra("asfasd") ? new ServiceBinder() : null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public class ServiceBinder extends Binder {
        public TestVPNService getService(){
            return TestVPNService.this;
        }
    }

    public static Intent getDestroyIntent(Context context){
        return null;
    }

    public static Intent getDestroyIntent(Context context, String reason){
        return null;
    }

    public static Intent getStartVPNIntent(Context context){
        return new Intent(context, TestVPNService.class).putExtra("asddsa",true);
    }

    public static Intent getStartVPNIntent(Context context, boolean startedWithTasker){
        return new Intent(context, TestVPNService.class).putExtra("asddsa",true);
    }

    public static Intent getStopVPNIntent(Context context){
        return null;
    }

    public static Intent getStartVPNIntent(Context context, String dns1, String dns2, String dns1v6, String dns2v6, boolean startedWithTasker, boolean fixedDNS){
        return null;
    }

    public static Intent getStartVPNIntent(Context context, String dns1, String dns2, String dns1v6, String dns2v6, boolean startedWithTasker){
        return null;
    }

    public static Intent getBinderIntent(Context context){
        return null;
    }

    public String getCurrentDNS1() {
        return null;
    }

    public String getCurrentDNS2() {
        return null;
    }

    public String getCurrentDNS1V6() {
        return null;
    }

    public String getCurrentDNS2V6() {
        return null;
    }
    public boolean startedFromShortcut(){
        return false;
    }

    public static boolean isDNSThreadRunning(){
        return false;
    }

    public static boolean isServiceRunning(){
        return false;
    }
}
