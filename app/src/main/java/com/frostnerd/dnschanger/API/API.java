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
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.service.quicksettings.TileService;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tiles.TilePause;
import com.frostnerd.dnschanger.tiles.TileResume;
import com.frostnerd.dnschanger.tiles.TileStart;
import com.frostnerd.dnschanger.tiles.TileStop;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private static final List<DNSEntry> defaultDNSEntries = new ArrayList<>();
    private static final HashMap<String, DNSEntry> additionalDefaultEntries = new HashMap<>();

    static {
        defaultDNSEntries.add(new DNSEntry(0, "Google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844", ""));
        defaultDNSEntries.add(new DNSEntry(0, "OpenDNS", "208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Level3", "209.244.0.3", "209.244.0.4", "", "", ""));
        defaultDNSEntries.add(new DNSEntry(0, "FreeDNS", "37.235.1.174", "37.235.1.177", "", "", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Yandex", "77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Verisign", "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Alternate", "198.101.242.72", "23.253.163.53", "", "", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security", "199.85.126.10", "199.85.127.10", "", "", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Pornography", "199.85.126.20", "199.85.127.20", "", "", ""));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Portnography + Other", "199.85.126.30", "199.85.127.30", "", "", ""));
        Collections.sort(defaultDNSEntries);

        additionalDefaultEntries.put("unblockr", new DNSEntry(0, "Unblockr", "178.62.57.141", "139.162.231.18", "", "", "Non-public DNS server for kodi. Visit unblockr.net for more information."));
    }

    public static synchronized void updateTiles(Context context) {
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Trying to update Tiles");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStart.class));
            TileService.requestListeningState(context, new ComponentName(context, TileResume.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePause.class));
            TileService.requestListeningState(context, new ComponentName(context, TileStop.class));
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Tiles updated");
        } else
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Not updating Tiles (Version is below Android N)");
    }

    // This is dirty. Like really dirty. But sometimes the running check returns running when the
    // service isn't running. This is a workaround.
    public static boolean isServiceRunning(Context c) {
        return DNSVpnService.isServiceRunning() || isServiceRunningNative(c);
        /*ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;*/
    }

    private static boolean isServiceRunningNative(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIPv6Enabled(Context context) {
        return Preferences.getBoolean(context, "setting_ipv6_enabled", true);
    }

    public static String getDNS1(Context context) {
        return Preferences.getString(context, "dns1", "8.8.8.8");
    }

    public static String getDNS2(Context context) {
        return Preferences.getString(context, "dns2", "8.8.4.4");
    }

    public static String getDNS1V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns1-v6", "2001:4860:4860::8888") : "";
    }

    public static String getDNS2V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns2-v6", "2001:4860:4860::8844") : "";
    }

    public static boolean isServiceThreadRunning(Context context) {
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
        Preferences.setDebug(true);
        database = context.openOrCreateDatabase("data.db", SQLiteDatabase.OPEN_READWRITE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS Shortcuts(Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT)");
        database.execSQL("CREATE TABLE IF NOT EXISTS DNSEntries(ID INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT,description TEXT DEFAULT '')");
        if (!Preferences.getBoolean(context, "dnsentries_created", false)) {
            List<DNSEntry> prev = loadDNSEntriesFromDatabase(context);
            defaultDNSEntries.addAll(prev);
            database.execSQL("DROP TABLE DNSEntries");
            database.execSQL("CREATE TABLE IF NOT EXISTS DNSEntries(ID INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT, dns1 TEXT, dns2 TEXT, " +
                    "dns1v6 TEXT, dns2v6 TEXT, description TEXT DEFAULT '')");
            for (DNSEntry entry : defaultDNSEntries) {
                ContentValues values = new ContentValues(5);
                values.put("Name", entry.getName());
                values.put("dns1", entry.getDns1());
                values.put("dns2", entry.getDns2());
                values.put("dns1v6", entry.getDns1V6());
                values.put("dns2v6", entry.getDns2V6());
                values.put("description", entry.getDescription());
                database.insert("DNSEntries", null, values);
            }
            for (String s : additionalDefaultEntries.keySet()) {
                DNSEntry entry = additionalDefaultEntries.get(s);
                ContentValues values = new ContentValues(5);
                values.put("Name", entry.getName());
                values.put("dns1", entry.getDns1());
                values.put("dns2", entry.getDns2());
                values.put("dns1v6", entry.getDns1V6());
                values.put("dns2v6", entry.getDns2V6());
                values.put("description", entry.getDescription());
                database.insert("DNSEntries", null, values);
                Preferences.put(context, "set_" + s, true);
            }
            Preferences.put(context, "dnsentries_created", true);
            Preferences.put(context, "dnsentries_description", true);
        } else {
            for (String s : additionalDefaultEntries.keySet()) {
                if (!Preferences.getBoolean(context, "set_" + s, false)) {
                    DNSEntry entry = additionalDefaultEntries.get(s);
                    ContentValues values = new ContentValues(5);
                    values.put("Name", entry.getName());
                    values.put("dns1", entry.getDns1());
                    values.put("dns2", entry.getDns2());
                    values.put("dns1v6", entry.getDns1V6());
                    values.put("dns2v6", entry.getDns2V6());
                    values.put("description", entry.getDescription());
                    database.insert("DNSEntries", null, values);
                    Preferences.put(context, "set_" + s, true);
                }
            }
            if (!Preferences.getBoolean(context, "dnsentries_description", false)) {
                database.execSQL("ALTER TABLE DNSEntries ADD COLUMN description TEXT DEFAULT ''");
                Preferences.put(context, "dnsentries_description", true);
            }
        }
    }

    public static List<API.DNSEntry> loadDNSEntriesFromDatabase(Context context) {
        List<API.DNSEntry> entries = new ArrayList<>();
        try {
            SQLiteDatabase database = API.getDatabase(context);
            Cursor cursor = database.rawQuery("SELECT * FROM DNSEntries", new String[]{});

            if (cursor.moveToFirst()) {
                do {
                    entries.add(new API.DNSEntry(cursor.getInt(cursor.getColumnIndex("ID")), cursor.getString(cursor.getColumnIndex("Name")), cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
                            cursor.getString(cursor.getColumnIndex("dns1v6")), cursor.getString(cursor.getColumnIndex("dns2v6")),
                            cursor.getString(cursor.getColumnIndex("description"))));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            //This is here because the table could possibly not exist when creating the table but the entries are still queried
        }
        return entries;
    }

    public static void removeDNSEntry(int ID) {
        database.execSQL("DELETE FROM DNSEntries WHERE ID=" + ID);
    }

    public static synchronized void deleteDatabase(Context context) {
        context.getDatabasePath("data.db").delete();
        database = null;
    }

    public static synchronized SQLiteDatabase getDatabase(Context context) {
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

    public static void updateAppShortcuts(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            if (!Preferences.getBoolean(context, "setting_app_shortcuts_enabled", false)) {
                shortcutManager.removeAllDynamicShortcuts();
                return;
            }
            boolean pinProtected = Preferences.getBoolean(context, "pin_app_shortcut", false);
            List<ShortcutInfo> shortcutInfos = new ArrayList<>();
            if (isServiceThreadRunning(context)) {
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

    public static int resolveColor(Context context, int attrID) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrID, typedValue, true);
        return typedValue.data;
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

    public static class DNSEntry implements Comparable<DNSEntry> {
        private String name, dns1, dns2, dns1V6, dns2V6, description;
        private int ID;

        public DNSEntry(int id, String name, String dns1, String dns2, String dns1V6, String dns2V6, String description) {
            this.name = name;
            this.dns1 = dns1;
            this.dns2 = dns2;
            this.dns1V6 = dns1V6;
            this.dns2V6 = dns2V6;
            this.ID = id;
            this.description = description;
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

        public String getDns1V6() {
            return dns1V6;
        }

        public String getDns2V6() {
            return dns2V6;
        }

        public String getDescription() {
            return description;
        }

        public int getID() {
            return ID;
        }

        @Override
        public int compareTo(@NonNull DNSEntry o) {
            return name.compareTo(o.name);
        }
    }
}
