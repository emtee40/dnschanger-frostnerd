package com.frostnerd.dnschanger.API;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.TypedValue;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tiles.TilePauseResume;
import com.frostnerd.dnschanger.tiles.TileStartStop;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String BROADCAST_SHORTCUT_CREATED = "com.frostnerd.dnschanger.SHORTCUT_CREATED";
    public static final String LOG_TAG = "[API]";
    private static DatabaseHelper dbHelper;

    public static synchronized void updateTiles(Context context) {
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Trying to update Tiles");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            TileService.requestListeningState(context, new ComponentName(context, TileStartStop.class));
            TileService.requestListeningState(context, new ComponentName(context, TilePauseResume.class));
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Tiles updated");
        } else
            LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Not updating Tiles (Version is below Android N)");
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

    public static boolean isServiceRunning(Context context, Class serviceClass){
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
        return Preferences.getBoolean(context, "setting_ipv6_enabled", false);
    }

    public static boolean isIPv4Enabled(Context context) {
        return Preferences.getBoolean(context, "setting_ipv4_enabled", true);
    }

    public static String getDNS1(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns1", "8.8.8.8") : "";
    }

    public static String getDNS2(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns2", "8.8.4.4") : "";
    }

    public static String getDNS1V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns1-v6", "2001:4860:4860::8888") : "";
    }


    public static String getDNS2V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns2-v6", "2001:4860:4860::8844") : "";
    }

    public static boolean isServiceThreadRunning() {
        return DNSVpnService.isDNSThreadRunning();
    }

    public static boolean isTaskerInstalled(Context context) {
        return Utils.isPackageInstalled(context, "net.dinglisch.android.taskerm");
    }

    public static void terminate() {
        if (dbHelper != null) dbHelper.close();
    }

    public static DatabaseHelper getDBHelper(Context context) {
        return dbHelper == null ?
                (dbHelper = (Preferences.getBoolean(context, "legacy_backup", false) ?
                        new DatabaseHelper(context) :
                        new DatabaseHelper(context, getDNSEntries(context)))) :
                dbHelper;
    }

    private static SQLiteDatabase getLegacyDatabase(Context context) {
        return context.openOrCreateDatabase("data.db", SQLiteDatabase.OPEN_READWRITE, null);
    }

    private static List<DNSEntry> getDNSEntries(Context context) {
        List<DNSEntry> entries = new ArrayList<>();
        try {
            Cursor cursor = getLegacyDatabase(context).rawQuery("SELECT * FROM DNSEntries", new String[]{});
            if (cursor.moveToFirst()) {
                do {
                    entries.add(new DNSEntry(cursor.getInt(cursor.getColumnIndex("ID")), cursor.getString(cursor.getColumnIndex("Name")), cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
                            cursor.getString(cursor.getColumnIndex("dns1v6")), cursor.getString(cursor.getColumnIndex("dns2v6")),
                            cursor.getString(cursor.getColumnIndex("description")), true));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            //This is here because the table could possibly not exist when creating the table but the entries are still queried
        }
        return entries;
    }

    /*private static synchronized void setupDatabase(Context context) {
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
    }*/

    public static synchronized void deleteDatabase(Context context) {
        if(dbHelper != null)dbHelper.close();
        dbHelper = null;
        context.deleteDatabase("data");
        context.getDatabasePath("data.db").delete();
    }

    public static int resolveColor(Context context, int attrID) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrID, typedValue, true);
        return typedValue.data;
    }

    public static void createShortcut(Context context, Shortcut shortcut) {
        if (shortcut == null) return;
        createShortcut(context, shortcut.getDns1(), shortcut.getDns2(), shortcut.getDns1v6(), shortcut.getDns2v6(), shortcut.getName());
    }

    public static void createShortcut(Context context, String dns1, String dns2, String dns1V6, String dns2V6, String name) {
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG}, "Creating shortcut");
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
        shortcutIntent.setAction("com.frostnerd.dnschanger.RUN_VPN_FROM_SHORTCUT");
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        shortcutIntent.putExtra("dns1", dns1);
        shortcutIntent.putExtra("dns2", dns2);
        shortcutIntent.putExtra("dns1v6", dns1V6);
        shortcutIntent.putExtra("dns2v6", dns2V6);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Activity.SHORTCUT_SERVICE);
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(context, StringUtil.randomString(30))
                        .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                        .setShortLabel(name)
                        .setLongLabel(name)
                        .setIntent(shortcutIntent)
                        .build();
                PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(API.BROADCAST_SHORTCUT_CREATED), 0);
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

    public static boolean isAnyVPNRunning(Context context){
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)return isAnyVPNRunningV21(context);
        else{
            try {
                for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if(networkInterface.getName().equals("tun0"))return true;
                }
            } catch (Exception ignored) {

            }
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static boolean isAnyVPNRunningV21(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for (Network network : cm.getAllNetworks()) {
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            if(caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))return true;
        }
        return false;
    }

    public static void startService(Context context, Intent intent){
        if(Preferences.getBoolean(context, "everything_disabled", false))return;
        if(intent.getComponent().getClassName().equals(DNSVpnService.class.getName()) &&
                Preferences.getBoolean(context, "setting_show_notification", true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(intent);
        }else context.startService(intent);
    }

    public static void startDNSServerConnectivityCheck(@NonNull final Context context, @NonNull final ConnectivityCheckCallback callback){
        runAsyncDNSQuery(isIPv4Enabled(context) ? getDNS1(context) : getDNS1V6(context), "frostnerd.com", false, Type.A, DClass.ANY, new DNSQueryResultListener() {
            @Override
            public void onSuccess(Message response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void startDNSServerConnectivityCheck(@NonNull final Context context, @NonNull final String dnsAddress, @NonNull final ConnectivityCheckCallback callback){
        runAsyncDNSQuery(dnsAddress, "frostnerd.com", false, Type.A, DClass.ANY, new DNSQueryResultListener() {
            @Override
            public void onSuccess(Message response) {
                callback.onCheckDone(true);
            }

            @Override
            public void onError(@Nullable Exception e) {
                callback.onCheckDone(false);
            }
        }, 2);
    }

    public static void runAsyncDNSQuery(final String server, final String query, final boolean tcp, final int type,
                                        final int dClass, final DNSQueryResultListener resultListener, final int timeout){
        new Thread(){
            @Override
            public void run() {
                try {
                    Resolver resolver = new SimpleResolver(server);
                    resolver.setTCP(tcp);
                    resolver.setTimeout(timeout);
                    Name name = Name.fromString(query.endsWith(".") ? query : query + ".");
                    Record record = Record.newRecord(name, type, dClass);
                    Message query = Message.newQuery(record);
                    Message response = resolver.send(query);
                    if(response.getSectionRRsets(1) == null)throw new IllegalStateException("Answer is null");
                    resultListener.onSuccess(response);
                } catch (IOException e) {
                    resultListener.onError(e);
                }
            }
        }.start();
    }

    public static Message runSyncDNSQuery(final String server, final String query, final boolean tcp, final int type,
                                        final int dClass, final int timeout){
                try {
                    Resolver resolver = new SimpleResolver(server);
                    resolver.setTCP(tcp);
                    resolver.setTimeout(timeout);
                    Name name = Name.fromString(query.endsWith(".") ? query : query + ".");
                    Record record = Record.newRecord(name, type, dClass);
                    Message mquery = Message.newQuery(record);
                    Message response = resolver.send(mquery);
                    if(response.getSectionRRsets(1) == null)throw new IllegalStateException("Answer is null");
                    return response;
                } catch (IOException e) {
                    return null;
                }
    }

    public interface ConnectivityCheckCallback{
        public void onCheckDone(boolean result);
    }

    public interface DNSQueryResultListener{
        public void onSuccess(Message response);
        public void onError(@Nullable Exception e);
    }
}
