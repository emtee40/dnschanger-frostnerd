package com.frostnerd.dnschanger.API;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final List<DNSEntry> defaultDNSEntries = new ArrayList<>();
    private static final HashMap<String, DNSEntry> additionalDefaultEntries = new HashMap<>();
    private Context context;
    static {
        defaultDNSEntries.add(new DNSEntry(0, "Google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "OpenDNS", "208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Level3", "209.244.0.3", "209.244.0.4", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "FreeDNS", "37.235.1.174", "37.235.1.177", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Yandex", "77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Verisign", "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Alternate", "198.101.242.72", "23.253.163.53", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security", "199.85.126.10", "199.85.127.10", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Pornography", "199.85.126.20", "199.85.127.20", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Pornography + Other", "199.85.126.30", "199.85.127.30", "", "", "",false));
        Collections.sort(defaultDNSEntries);

        additionalDefaultEntries.put("unblockr", new DNSEntry(0, "Unblockr", "178.62.57.141", "139.162.231.18", "", "", "Non-public DNS server for kodi. Visit unblockr.net for more information.",false));
    }
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase currentDB;
    private boolean isCreating;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public DatabaseHelper(Context context, List<DNSEntry> entries){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if(entries.size() != 0){
            defaultDNSEntries.clear();
            additionalDefaultEntries.clear();
            defaultDNSEntries.addAll(entries);
        }
        this.context = context;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        isCreating = true;
        currentDB = db;
        db.execSQL("CREATE TABLE Shortcuts(Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT)");
        db.execSQL("CREATE TABLE DNSEntries(ID INTEGER PRIMARY KEY AUTOINCREMENT,Name TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT,description TEXT DEFAULT '')");
        for(DNSEntry entry: defaultDNSEntries){
            saveDNSEntry(entry);
        }
        for (String s : additionalDefaultEntries.keySet()) {
            saveDNSEntry(additionalDefaultEntries.get(s));
        }
        Preferences.put(context, "dnsentries_description", true);
        Preferences.put(context, "dnsentries_created", true);
        Preferences.put(context, "legacy_backup", true);
        isCreating = false;
        currentDB = null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 2){
            db.execSQL("ALTER TABLE DNSEntries ADD COLUMN CustomEntry BOOLEAN DEFAULT ''");
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if(isCreating && currentDB != null){
            return currentDB;
        }
        return super.getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if(isCreating && currentDB != null){
            return currentDB;
        }
        return super.getReadableDatabase();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!Preferences.getBoolean(context, "dnsentries_created", false)) {
            getWritableDatabase().execSQL("DELETE FROM DNSEntries");
            for(DNSEntry entry: defaultDNSEntries){
                saveDNSEntry(entry);
            }
            for (String s : additionalDefaultEntries.keySet()) {
                saveDNSEntry(additionalDefaultEntries.get(s));
            }
            Preferences.put(context, "dnsentries_created", true);
        }
        if (!Preferences.getBoolean(context, "dnsentries_description", false)) {
            getWritableDatabase().execSQL("ALTER TABLE DNSEntries ADD COLUMN description TEXT DEFAULT ''");
            Preferences.put(context, "dnsentries_description", true);
        }
    }

    public synchronized void saveDNSEntry(DNSEntry entry){
        ContentValues values = new ContentValues(5);
        values.put("Name", entry.getName());
        values.put("dns1", entry.getDns1());
        values.put("dns2", entry.getDns2());
        values.put("dns1v6", entry.getDns1V6());
        values.put("dns2v6", entry.getDns2V6());
        values.put("description", entry.getDescription());
        values.put("CustomEntry", entry.isCustomEntry());
        getWritableDatabase().insert("DNSEntries", null, values);
    }

    public synchronized void removeDNSEntry(int ID){
        getWritableDatabase().execSQL("DELETE FROM DNSEntries WHERE ID=" + ID);
    }

    public synchronized void saveShortcut(String dns1, String dns2, String dns1V6, String dns2V6, String name) {
        ContentValues values = new ContentValues();
        values.put("dns1", dns1);
        values.put("dns2", dns2);
        values.put("dns1v6", dns1V6);
        values.put("dns2v6", dns2V6);
        values.put("Name", name);
        getWritableDatabase().insert("Shortcuts", null, values);
    }

    public synchronized Shortcut[] getShortcuts() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM Shortcuts", new String[]{});
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

    public synchronized List<DNSEntry> getDNSEntries(){
        List<DNSEntry> entries = new ArrayList<>();
        try {
            Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM DNSEntries", new String[]{});
            if (cursor.moveToFirst()) {
                do {
                    entries.add(new DNSEntry(cursor.getInt(cursor.getColumnIndex("ID")), cursor.getString(cursor.getColumnIndex("Name")), cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
                            cursor.getString(cursor.getColumnIndex("dns1v6")), cursor.getString(cursor.getColumnIndex("dns2v6")),
                            cursor.getString(cursor.getColumnIndex("description")), cursor.getInt(cursor.getColumnIndex("CustomEntry")) == 1));
                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }
}
