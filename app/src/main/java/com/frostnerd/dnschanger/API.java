package com.frostnerd.dnschanger;

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
import android.support.v4.content.ContextCompat;

import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;

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

    public static void updateAllWidgets(Context context, Class<? extends AppWidgetProvider> providerClass) {
        LogFactory.writeMessage(context, LOG_TAG, "Updating all Widgets of provider " + providerClass);
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, providerClass));
        for(int i: ids)updateWidget(context, providerClass, i);
        LogFactory.writeMessage(context, LOG_TAG, ids.length + " Widgets updated.");
    }

    public static void updateWidget(Context context, Class<? extends AppWidgetProvider> providerClass, int widgetID){
        Intent intent = new Intent(context,providerClass);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = {widgetID};
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        context.sendBroadcast(intent);
    }

    public static String randomLocalIPv6Address() {
        String prefix = randomIPv6LocalPrefix();
        for (int i = 0; i < 5; i++) prefix += ":" + randomIPv6Block(16, false);
        return prefix;
    }

    private static String randomIPv6LocalPrefix() {
        return "fd" + randomIPv6Block(8, true) + ":" + randomIPv6Block(16, false) + ":" + randomIPv6Block(16, false);
    }

    private static String randomIPv6Block(int bits, boolean leading_zeros) {
        String hex = Long.toHexString((long) Math.floor(Math.random() * Math.pow(2, bits)));
        if (!leading_zeros || hex.length() == bits / 4) ;
        hex = "0000".substring(0, bits / 4 - hex.length()) + hex;
        return hex;
    }

    public static boolean checkVPNServiceRunning(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTaskerInstalled(Context context) {
        List<ApplicationInfo> packages;
        packages = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals("net.dinglisch.android.taskerm")) return true;
        }
        return false;
    }

    public static boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return true;
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean canWriteExternalStorage(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReadExternalStorage(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void terminate() {
        if (database != null) database.close();
    }

    private static void setupDatabase(Context context) {
        if (database != null) return;
        database = context.openOrCreateDatabase("data.db", SQLiteDatabase.OPEN_READWRITE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS Shortcuts(Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT)");
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

    public static Shortcut[] getShortcutsFromDatabase(Context context) {
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
