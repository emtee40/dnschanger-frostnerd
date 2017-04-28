package com.frostnerd.dnschanger.API;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.support.v4.content.ContextCompat;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tiles.TilePause;
import com.frostnerd.dnschanger.tiles.TileResume;
import com.frostnerd.dnschanger.tiles.TileStart;
import com.frostnerd.dnschanger.tiles.TileStop;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public final class API {
    public static final String BROADCAST_SERVICE_STATUS_CHANGE = "com.frostnerd.dnschanger.VPN_SERVICE_CHANGE";
    public static final String BROADCAST_SERVICE_STATE_REQUEST = "com.frostnerd.dnschanger.VPN_STATE_CHANGE";
    public static final String LOG_TAG = "[API]";
    private static SQLiteDatabase database;

    public static synchronized void updateTiles(Context context){
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Trying to update Tiles");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStart.class));
            TileService.requestListeningState(context, new ComponentName(context, TileResume.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePause.class));
            TileService.requestListeningState(context, new ComponentName(context, TileStop.class));
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Tiles updated");
        }else LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Not updating Tiles (Version is below Android N)");
    }

    // This is dirty. Like really dirty. But sometimes the running check returns running when the
    // service isn't running. This is a workaround.
    public static boolean isServiceRunning(Context c) {
        return DNSVpnService.isServiceRunning() ||isServiceRunningNative(c);
        /*ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;*/
    }

    private static boolean isServiceRunningNative(Context context){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIPv6Enabled(Context context){
        return Preferences.getBoolean(context, "setting_ipv6_enabled", true);
    }

    public static String getDNS1(Context context){
        return Preferences.getString(context, "dns1", "8.8.8.8");
    }

    public static String getDNS2(Context context){
        return Preferences.getString(context, "dns2", "8.8.4.4");
    }

    public static String getDNS1V6(Context context){
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns1-v6", "2001:4860:4860::8888") : "";
    }

    public static String getDNS2V6(Context context){
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns2-v6", "2001:4860:4860::8844") : "";
    }

    public static boolean isServiceThreadRunning(Context context){
        return DNSVpnService.isDNSThreadRunning();
    }

    public static boolean isTaskerInstalled(Context context) {
        return Utils.isPackageInstalled(context, "net.dinglisch.android.taskerm");
    }

    public static void terminate() {
        if (database != null) database.close();
    }

    private static synchronized void setupDatabase(Context context) {
        if (database != null) return;
        database = context.openOrCreateDatabase("data.db", SQLiteDatabase.OPEN_READWRITE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS Shortcuts(Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT)");
        database.execSQL("CREATE TABLE IF NOT EXISTS DNSEntries(Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT)");
    }

    public static synchronized SQLiteDatabase getDatabase(Context context){
        setupDatabase(context);
        return database;
    }

    public static void onShortcutCreated(Context context, String dns1, String dns2, String dns1V6, String dns2V6, String name) {
        setupDatabase(context);
        ContentValues values = new ContentValues();
        values.put("dns1", dns1);
        values.put("dns2", dns2);
        values.put("dns1v6", dns1V6);
        values.put("dns2v6", dns2V6);
        values.put("Name", name);
        database.insert("Shortcuts", null, values);
    }

    public static void createShortcut(Context context, Shortcut shortcut) {
        if (shortcut == null) return;
        createShortcut(context, shortcut.dns1, shortcut.dns2, shortcut.dns1v6, shortcut.dns2v6, shortcut.name);
    }

    public static void createShortcut(Context context, String dns1, String dns2, String dns1V6, String dns2V6, String name) {
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Creating shortcut");
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.putExtra("dns1", dns1);
        shortcutIntent.putExtra("dns2", dns2);
        shortcutIntent.putExtra("dns1v6", dns1V6);
        shortcutIntent.putExtra("dns2v6", dns2V6);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Adding shortcut", shortcutIntent);
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Intent for adding to Screen:", addIntent);
        context.sendBroadcast(addIntent);
    }

    public static synchronized Shortcut[] getShortcutsFromDatabase(Context context) {
        setupDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM Shortcuts", new String[]{});
        if (cursor.moveToFirst()) {
            Shortcut[] shortcuts = new Shortcut[cursor.getCount()];
            int i = 0;
            do {
                shortcuts[i++] = new Shortcut(cursor.getString(cursor.getColumnIndex("Name")), cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
                        cursor.getString(cursor.getColumnIndex("dns1v6")), cursor.getString(cursor.getColumnIndex("dns2v6")));
            } while (cursor.moveToNext());
            return shortcuts;
        } else {
            cursor.close();
            return new Shortcut[]{};
        }
    }

    public static class Shortcut {
        private String name, dns1, dns2, dns1v6, dns2v6;

        public Shortcut(String name, String dns1, String dns2, String dns1v6, String dns2v6) {
            this.name = name;
            this.dns1 = dns1;
            this.dns2 = dns2;
            this.dns1v6 = dns1v6;
            this.dns2v6 = dns2v6;
        }

        public String getName() {
            return name;
        }

        public String getDns1() {
            return dns1;
        }

        public String getDns2() {
            return dns2;
        }

        public String getDns1v6() {
            return dns1v6;
        }

        public String getDns2v6() {
            return dns2v6;
        }

        @Override
        public String toString() {
            return dns1 + "<<>>" + dns2 + "<<>>" + dns1v6 + "<<>>" + dns2v6 + "<<>>" + name;
        }

        public static Shortcut fromString(String s) {
            if (s == null || s.equals("") || s.split("<<>>").length < 5) return null;
            String[] splt = s.split("<<>>");
            return new Shortcut(splt[4], splt[0], splt[1], splt[2], splt[3]);
        }
    }
}
