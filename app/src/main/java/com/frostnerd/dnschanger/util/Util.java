package com.frostnerd.dnschanger.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.service.quicksettings.TileService;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.dnschanger.services.ConnectivityBackgroundService;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.services.jobs.ConnectivityJobAPI21;
import com.frostnerd.dnschanger.tiles.TilePauseResume;
import com.frostnerd.dnschanger.tiles.TileStartStop;
import com.frostnerd.general.StringUtil;
import com.frostnerd.general.Utils;
import com.frostnerd.networking.NetworkUtil;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.measite.minidns.Record;
import de.measite.minidns.record.Data;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public final class Util {
    public static final String BROADCAST_SERVICE_STATUS_CHANGE = "com.frostnerd.dnschanger.VPN_SERVICE_CHANGE";
    public static final String BROADCAST_SERVICE_STATE_REQUEST = "com.frostnerd.dnschanger.VPN_STATE_CHANGE";
    public static final String BROADCAST_SHORTCUT_CREATED = "com.frostnerd.dnschanger.SHORTCUT_CREATED";
    private static final String LOG_TAG = "[Util]";
    static final Pattern ipv6WithPort = Pattern.compile("(\\[[0-9a-f:]+]:[0-9]{1,5})|([0-9a-f:]+)");
    static final Pattern ipv4WithPort = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]{1,5})?");

    public static synchronized void updateTiles(Context context) {
        if(context == null)throw new IllegalStateException("The context passed to updateTiles is null.");
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Trying to update Tiles");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStartStop.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePauseResume.class));
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Tiles updated");
        } else
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Not updating Tiles (Version is below Android N)");
    }

    public static IPPortPair validateInput(String input, boolean iPv6, boolean allowEmpty, boolean allowLoopback, int defaultPort) {
        if (allowEmpty && input.equals("")) return IPPortPair.getEmptyPair();
        if (iPv6) {
            if (ipv6WithPort.matcher(input).matches()) {
                if (input.contains("[")) {
                    int port = Integer.parseInt(input.split("]")[1].split(":")[1]);
                    String address = input.split("]")[0].replace("[", "");
                    boolean addressValid = (allowLoopback && NetworkUtil.isIP(address, true)) || NetworkUtil.isAssignableAddress(address, true);
                    return port <= 65535 && port >= 1 && addressValid ? new IPPortPair(address, port, true) : null;
                } else {
                    boolean addressValid = (allowLoopback && NetworkUtil.isIP(input, true)) || NetworkUtil.isAssignableAddress(input, true);
                    return addressValid ? new IPPortPair(input, defaultPort, true) : null;
                }
            } else {
                return null;
            }
        } else {
            if (ipv4WithPort.matcher(input).matches()) {
                if (input.contains(":")) {
                    int port = Integer.parseInt(input.split(":")[1]);
                    String address = input.split(":")[0];
                    boolean addressValid = (allowLoopback && NetworkUtil.isIP(address, false)) || NetworkUtil.isAssignableAddress(address, false);
                    return port <= 65535 && port >= 1 && addressValid ? new IPPortPair(address, port, false) : null;
                } else {
                    boolean addressValid = (allowLoopback && NetworkUtil.isIP(input, false)) || NetworkUtil.isAssignableAddress(input, false);
                    return addressValid ? new IPPortPair(input, defaultPort, false) : null;
                }
            } else {
                return null;
            }
        }
    }

    public static IPPortPair validateInput(String input, boolean iPv6, boolean allowEmpty, int defPort) {
        return validateInput(input, iPv6, allowEmpty, false, defPort);
    }

    public static void updateAppShortcuts(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = Utils.requireNonNull(context.getSystemService(ShortcutManager.class));
            if (!PreferencesAccessor.areAppShortcutsEnabled(context)) {
                shortcutManager.removeAllDynamicShortcuts();
                return;
            }
            boolean pinProtected = PreferencesAccessor.isPinProtected(context, PreferencesAccessor.PinProtectable.APP_SHORTCUT);
            List<ShortcutInfo> shortcutInfos = new ArrayList<>();
            if (isServiceThreadRunning()) {
                Bundle extras1 = new Bundle();
                extras1.putBoolean("stop_vpn", true);
                extras1.putBoolean("redirectToService", true);
                Bundle extras2 = new Bundle();
                extras2.putBoolean("destroy", true);
                extras2.putBoolean("redirectToService", true);
                shortcutInfos.add(new ShortcutInfo.Builder(context, "id1").setShortLabel(context.getString(R.string.tile_pause))
                        .setLongLabel(context.getString(R.string.tile_pause)).setIcon(Icon.createWithResource(context, R.drawable.ic_stat_pause_dark))
                        .setIntent(pinProtected ? new Intent(context.getApplicationContext(), PinActivity.class).putExtras(extras1).setAction(StringUtil.randomString(40)) : DNSVpnService.getStopVPNIntent(context.getApplicationContext())).build());
                shortcutInfos.add(new ShortcutInfo.Builder(context, "id2").setShortLabel(context.getString(R.string.tile_stop))
                        .setLongLabel(context.getString(R.string.tile_stop)).setIcon(Icon.createWithResource(context, R.drawable.ic_stat_stop_dark))
                        .setIntent(pinProtected ? new Intent(context.getApplicationContext(), PinActivity.class).putExtras(extras2).setAction(StringUtil.randomString(40)) : DNSVpnService.getDestroyIntent(context.getApplicationContext())).build());
            } else if (isServiceRunning(context)) {
                Bundle extras = new Bundle();
                extras.putBoolean("start_vpn", true);
                extras.putBoolean("redirectToService", true);
                shortcutInfos.add(new ShortcutInfo.Builder(context, "id3").setShortLabel(context.getString(R.string.tile_resume))
                        .setLongLabel(context.getString(R.string.tile_resume)).setIcon(Icon.createWithResource(context, R.drawable.ic_stat_resume_dark))
                        .setIntent(pinProtected ? new Intent(context.getApplicationContext(), PinActivity.class).putExtras(extras).setAction(StringUtil.randomString(40)) : DNSVpnService.getStartVPNIntent(context.getApplicationContext())).build());
            } else {
                Bundle extras = new Bundle();
                extras.putBoolean("start_vpn", true);
                extras.putBoolean("redirectToService", true);
                shortcutInfos.add(new ShortcutInfo.Builder(context, "id4").setShortLabel(context.getString(R.string.tile_start)).
                        setLongLabel(context.getString(R.string.tile_start)).setIcon(Icon.createWithResource(context, R.drawable.ic_stat_resume_dark))
                        .setIntent(pinProtected ? new Intent(context.getApplicationContext(), PinActivity.class).putExtras(extras).setAction(StringUtil.randomString(40)) : DNSVpnService.getStartVPNIntent(context.getApplicationContext())).build());
            }
            shortcutManager.setDynamicShortcuts(shortcutInfos);
        }
    }

    public static boolean isServiceRunning(Context c) {
        return DNSVpnService.isServiceRunning() || Utils.isServiceRunning(c, DNSVpnService.class);
    }

    public static boolean isServiceThreadRunning() {
        return DNSVpnService.isDNSThreadRunning();
    }

    public static boolean isTaskerInstalled(Context context) {
        return Utils.isPackageInstalled(context, "net.dinglisch.android.taskerm");
    }

    public static synchronized void deleteDatabase(Context context) {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        if(helper != null)helper.close();
        context.deleteDatabase("data");
        context.getDatabasePath("data.db").delete();
    }

    public static void createShortcut(Context context, Shortcut shortcut) {
        if (shortcut == null) return;
        ArrayList<IPPortPair> servers = new ArrayList<>(4);
        servers.add(shortcut.getDns1());
        servers.add(shortcut.getDns2());
        servers.add(shortcut.getDns1v6());
        servers.add(shortcut.getDns2v6());
        createShortcut(context, servers, shortcut.getName());
    }

    public static void createShortcut(Context context, ArrayList<IPPortPair> servers, String name) {
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Creating shortcut");
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
        shortcutIntent.setAction("com.frostnerd.dnschanger.RUN_VPN_FROM_SHORTCUT");
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.putExtra("servers", serializableToString(servers));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = Utils.requireNonNull((ShortcutManager) context.getSystemService(Activity.SHORTCUT_SERVICE));
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, StringUtil.randomString(30))
                        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                        .setShortLabel(name)
                        .setLongLabel(name)
                        .setIntent(shortcutIntent)
                        .build();
                PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(Util.BROADCAST_SHORTCUT_CREATED), 0);
                shortcutManager.requestPinShortcut(shortcutInfo, intent.getIntentSender());
                return;
            }
        }
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Adding shortcut", shortcutIntent);
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Intent for adding to Screen:", addIntent);
        context.sendBroadcast(addIntent);
    }

    public static void startService(Context context, Intent intent){
        if(PreferencesAccessor.isEverythingDisabled(context))return;
        if(intent.getComponent() != null && intent.getComponent().getClassName().equals(DNSVpnService.class.getName()) &&
                PreferencesAccessor.isNotificationEnabled(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(intent);
        }else context.startService(intent);
    }

    public static String createNotificationChannel(Context context, boolean allowHiding){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = Utils.requireNonNull((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE));
            if(allowHiding && PreferencesAccessor.shouldHideNotificationIcon(context)){
                NotificationChannel channel = new NotificationChannel("noIconChannel", context.getString(R.string.notification_channel_hiddenicon), NotificationManager.IMPORTANCE_MIN);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setDescription(context.getString(R.string.notification_channel_hiddenicon_description));
                channel.setImportance(NotificationManager.IMPORTANCE_MIN);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
                return "noIconChannel";
            }else{
                NotificationChannel channel = new NotificationChannel("defaultchannel", context.getString(R.string.notification_channel_default), NotificationManager.IMPORTANCE_LOW);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setDescription(context.getString(R.string.notification_channel_default_description));
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(channel);
                return "defaultchannel";
            }
        }else{
            return "defaultchannel";
        }
    }

    public static void runBackgroundConnectivityCheck(Context context, boolean handleInitialState){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogFactory.writeMessage(context, LOG_TAG, "Using JobScheduler");
            if(isJobRunning(context, 0)){
                LogFactory.writeMessage(context, LOG_TAG, "Job is already running/scheduled, not doing anything");
                return;
            }
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean("initial", handleInitialState);
            System.out.println("HANDLE INITIAL STATE: " + handleInitialState);
            JobScheduler scheduler = Utils.requireNonNull((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
            scheduler.schedule(new JobInfo.Builder(0, new ComponentName(context, ConnectivityJobAPI21.class)).setPersisted(true)
                    .setRequiresCharging(false).setMinimumLatency(0).setOverrideDeadline(0).setExtras(extras).build());
        } else {
            LogFactory.writeMessage(context, LOG_TAG, "Starting Service (Util below 21)");
            context.startService(new Intent(context, ConnectivityBackgroundService.class).putExtra("initial", handleInitialState));
        }
    }

    public static void stopBackgroundConnectivityCheck(Context context){
        LogFactory.writeMessage(context, LOG_TAG, "Stopping the background connectivity check..");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LogFactory.writeMessage(context, LOG_TAG, "Using JobScheduler");
            if(isJobRunning(context, 0)){
                LogFactory.writeMessage(context, LOG_TAG, "Job is running, stopping..");
                JobScheduler scheduler = Utils.requireNonNull((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
                scheduler.cancel(0);
            }else {
                LogFactory.writeMessage(context, LOG_TAG, "Job is not running, thus not stopping.");
            }
        } else {
            if(isBackgroundConnectivityCheckRunning(context)){
                LogFactory.writeMessage(context, LOG_TAG, "Stopping Service (API below 21)");
                context.stopService(new Intent(context, ConnectivityBackgroundService.class));
            } else {
                LogFactory.writeMessage(context, LOG_TAG, "Service is not running, thus not stopping.");
            }
        }
    }

    public static boolean isBackgroundConnectivityCheckRunning(@NonNull Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return isJobRunning(context, 0);
        } else {
            return Utils.isServiceRunning(context, ConnectivityBackgroundService.class);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static boolean isJobRunning(@NonNull Context context, @IntRange(from = 0) int jobID){
        JobScheduler scheduler = Utils.requireNonNull((JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE));
        for ( JobInfo jobInfo : scheduler.getAllPendingJobs())
            if (jobInfo.getId() == jobID) return true;
        return false;
    }

    /**
     * This Method is used instead of getActivity() in a fragment because getActivity() returns null in some rare cases
     * @param fragment
     * @return
     */
    public static FragmentActivity getActivity(Fragment fragment){
        if(fragment.getActivity() == null){
            if(fragment.getContext() != null && fragment.getContext() instanceof FragmentActivity){
                return (FragmentActivity)fragment.getContext();
            }else return null;
        }else return fragment.getActivity();
    }

    public static String serializableToString(Serializable serializable){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new Base64OutputStream(baos, Base64.NO_PADDING
                            | Base64.NO_WRAP));
            oos.writeObject(serializable);
            oos.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T extends Serializable> T serializableFromString(String s){
        try {
            return (T) new ObjectInputStream(new Base64InputStream(
                    new ByteArrayInputStream(s.getBytes()), Base64.NO_PADDING
                    | Base64.NO_WRAP)).readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface ConnectivityCheckCallback{
        void onCheckDone(boolean result);
    }

    public interface DNSQueryResultListener{
        void onSuccess(List<Record<? extends Data>> response);
        void onError(@Nullable Exception e);
    }
}