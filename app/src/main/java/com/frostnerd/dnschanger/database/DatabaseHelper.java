package com.frostnerd.dnschanger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;

import java.sql.Timestamp;
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
    static {
        defaultDNSEntries.add(new DNSEntry(0, "Google", "Google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "OpenDNS", "OpenDNS", "208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Level3", "Level3", "209.244.0.3", "209.244.0.4", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "FreeDNS", "FreeDNS", "37.235.1.174", "37.235.1.177", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Yandex", "Yandex", "77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Verisign", "Verisign", "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Alternate", "Alternate", "198.101.242.72", "23.253.163.53", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security", "Norton Connectsafe", "199.85.126.10", "199.85.127.10", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Pornography" , "Norton Connectsafe", "199.85.126.20", "199.85.127.20", "", "", "",false));
        defaultDNSEntries.add(new DNSEntry(0, "Norton Connectsafe - Security + Pornography + Other", "Norton Connectsafe", "199.85.126.30", "199.85.127.30", "", "", "",false));
        Collections.sort(defaultDNSEntries);

        additionalDefaultEntries.put("unblockr", new DNSEntry(0, "Unblockr", "Unblockr", "178.62.57.141", "139.162.231.18", "", "", "Non-public DNS server for kodi. Visit unblockr.net for more information.",false));
    }
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;
    private SQLiteDatabase currentDB;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        currentDB = db;
        db.execSQL("CREATE TABLE IF NOT EXISTS Shortcut(ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS DNSEntries(ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT, ShortName TEXT, dns1 TEXT, dns2 TEXT, dns1v6 TEXT, dns2v6 TEXT,description TEXT DEFAULT '', CustomEntry BOOLEAN DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS DNSRules(Domain TEXT NOT NULL, IPv6 BOOL DEFAULT 0, Target TEXT NOT NULL, Wildcard BOOL DEFAULT 0, PRIMARY KEY(Domain, IPv6))");
        db.execSQL("CREATE TABLE IF NOT EXISTS DNSQueries(Host TEXT NOT NULL, IPv6 BOOL DEFAULT 0, Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(Host, Time))");
        db.execSQL("CREATE TABLE IF NOT EXISTS DNSRuleImports(Filename TEXT NOT NULL, Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, RowStart INTEGER, RowEnd INTEGER, PRIMARY KEY(Filename, Time))");
        db.execSQL("CREATE TABLE IF NOT EXISTS ShortcutAddress(ShortcutID INTEGER, Address VARCHAR(50)," +
                " Port INTEGER, IPv6 BOOL, FOREIGN KEY(ShortcutID) REFERENCES Shortcut(ID))");
        for(DNSEntry entry: defaultDNSEntries){
            saveDNSEntry(entry);
        }
        for (String s : additionalDefaultEntries.keySet()) {
            saveDNSEntry(additionalDefaultEntries.get(s));
        }
        currentDB = null;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        currentDB = db;
        if(oldVersion < 2){
            db.execSQL("ALTER TABLE DNSEntries ADD COLUMN ShortName TEXT");
            for(DNSEntry entry: getDNSEntries()){
                if(!entry.isCustomEntry()){
                    DNSEntry def = findDefaultEntryByName(entry.getName());
                    entry.setShortName(def == null ? entry.getName() : def.getShortName());
                }else entry.setShortName(entry.getName());
                editEntry(entry);
            }
            db.execSQL("CREATE TABLE IF NOT EXISTS DNSRule(Domain TEXT NOT NULL, IPv6 BOOL DEFAULT 0, Target TEXT NOT NULL, Wildcard BOOL DEFAULT 0, PRIMARY KEY(Domain, IPv6))");
            db.execSQL("CREATE TABLE IF NOT EXISTS DNSQuery(Host TEXT NOT NULL, IPv6 BOOL DEFAULT 0, Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(Host, Time))");
            db.execSQL("CREATE TABLE IF NOT EXISTS DNSRuleImport(Filename TEXT NOT NULL, Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, RowStart INTEGER, RowEnd INTEGER, PRIMARY KEY(Filename, Time))");

            Cursor cursor = db.rawQuery("SELECT * FROM Shortcuts", new String[]{});
            List<String[]> data = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do{
                    data.add(new String[]{cursor.getString(cursor.getColumnIndex("Name")), cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
                            cursor.getString(cursor.getColumnIndex("dns1v6")), cursor.getString(cursor.getColumnIndex("dns2v6"))});
                }while (cursor.moveToNext());
            }
            cursor.close();
            db.execSQL("DROP TABLE Shortcuts");
            db.execSQL("CREATE TABLE IF NOT EXISTS Shortcut(ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS ShortcutAddress(ShortcutID INTEGER, Address VARCHAR(50)," +
                    " Port INTEGER, IPv6 BOOL, FOREIGN KEY(ShortcutID) REFERENCES Shortcut(ID), PRIMARY KEY(ShortcutID))");
            ContentValues values = new ContentValues();
            int id;
            for(String[] d: data){
                values.put("Name", d[0]);
                id = (int)db.insert("Shortcut", null, values);
                values.clear();
                values.put("ShortcutID", id);
                values.put("Port", 53);
                for(int i = 1; i <= 4; i++){
                    if(d[i].equals(""))continue;
                    values.put("Address", d[i]);
                    db.insert("ShortcutAddress", null, values);
                }
            }
        }
        currentDB = null;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if(currentDB != null){
            return currentDB;
        }
        return super.getWritableDatabase();
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if(currentDB != null){
            return currentDB;
        }
        return super.getReadableDatabase();
    }

    private DNSEntry findDefaultEntryByName(String name){
        for(DNSEntry entry: defaultDNSEntries)if(entry.getName().equals(name))return entry;
        for(DNSEntry entry: additionalDefaultEntries.values())if(entry.getName().equals(name))return entry;
        return null;
    }

    public void createRuleEntry(String host, String target, boolean ipv6, boolean wildcard){
        ContentValues values = new ContentValues(3);
        values.put("Domain", host);
        values.put("IPv6", ipv6);
        values.put("Target", target);
        values.put("Wildcard", wildcard);
        getWritableDatabase().insert("DNSRules", null, values);
    }

    public boolean dnsRuleExists(String host){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT ROWID FROM DNSRules WHERE Domain=?",
                new String[]{host});
        int count = cursor.getCount();
        cursor.close();
        return count == 1;
    }

    public boolean dnsRuleExists(String host, boolean ipv6){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT ROWID FROM DNSRules WHERE Domain=? and IPv6=?",
                new String[]{host, ipv6 ? "1" : "0"});
        int count = cursor.getCount();
        cursor.close();
        return count == 1;
    }

    public void editDNSRule(String host, boolean ipv6, String newTarget){
        ContentValues values = new ContentValues(1);
        values.put("Target", newTarget);
        getWritableDatabase().update("DNSRules", values, "Domain=? AND IPv6=?", new String[]{host, ipv6 ? "1" : "0"});
    }

    public boolean deleteDNSRule(String host, boolean ipv6){
        return getWritableDatabase().delete("DNSRules", "Domain=? AND IPv6=?", new String[]{host, ipv6 ? "1" : "0"}) > 0;
    }

    public synchronized void saveDNSEntry(DNSEntry entry){
        ContentValues values = new ContentValues(7);
        values.put("Name", entry.getName());
        values.put("dns1", entry.getDns1());
        values.put("dns2", entry.getDns2());
        values.put("dns1v6", entry.getDns1V6());
        values.put("dns2v6", entry.getDns2V6());
        values.put("description", entry.getDescription());
        values.put("CustomEntry", entry.isCustomEntry());
        values.put("ShortName", entry.getShortName());
        getWritableDatabase().insert("DNSEntries", null, values);
    }

    public synchronized void editEntry(DNSEntry entry){
        ContentValues values = new ContentValues(7);
        values.put("Name", entry.getName());
        values.put("dns1", entry.getDns1());
        values.put("dns2", entry.getDns2());
        values.put("dns1v6", entry.getDns1V6());
        values.put("dns2v6", entry.getDns2V6());
        values.put("description", entry.getDescription());
        values.put("CustomEntry", entry.isCustomEntry());
        values.put("ShortName", entry.getShortName());
        getWritableDatabase().update("DNSEntries", values, "ID=" + entry.getID(), null);
    }

    public synchronized void removeDNSEntry(int ID){
        getWritableDatabase().execSQL("DELETE FROM DNSEntries WHERE ID=" + ID);
    }

    public synchronized void saveShortcut(ArrayList<IPPortPair> servers, String name) {
        ContentValues values = new ContentValues();
        values.put("Name", name);
        SQLiteDatabase db = getWritableDatabase();
        db.insert("Shortcut", null, values);
        values.clear();
        values.put("ShortcutID", getLastInsertedRowID(db));
        for(IPPortPair pair: servers){
            values.put("Address", pair.getAddress());
            values.put("Port", pair.getPort());
            values.put("IPv6", pair.isIpv6());
            db.insert("ShortcutAddress", null, values);
        }
    }

    public int getLastInsertedRowID(SQLiteDatabase db){
        Cursor cursor = db.rawQuery("SELECT last_insert_rowid()", null);
        int id = -1;
        if(cursor.moveToFirst())id = cursor.getInt(0);
        cursor.close();
        return id;
    }

    public synchronized void createDNSRuleImport(String filename, int rowIDStart, int rowIDEnd){
        ContentValues values = new ContentValues(3);
        values.put("Filename", filename);
        values.put("RowStart", rowIDStart);
        values.put("RowEnd", rowIDEnd);
        getWritableDatabase().insert("DNSRuleImports", null, values);
    }

    public int getHighestRowID(String table){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT MAX(ROWID) FROM " + table, null);
        int max = -1;
        if(cursor.moveToFirst()) max = cursor.getInt(0);
        cursor.close();
        return max;
    }

    public synchronized DNSRuleImport[] getDNSRuleImports(){
        Cursor cursor = getReadableDatabase().rawQuery("SELECT Filename, Time, ROWID FROM DNSRuleImports", new String[]{});
        if(cursor.moveToFirst()){
            DNSRuleImport[] imports = new DNSRuleImport[cursor.getCount()];
            int i = 0;
            do{
                imports[i++] = new DNSRuleImport(cursor.getString(0), Timestamp.valueOf(cursor.getString(1)), cursor.getInt(2));
            }while(cursor.moveToNext());
            cursor.close();
            return imports;
        }else{
            cursor.close();
            return new DNSRuleImport[]{};
        }
    }

    public synchronized List<Shortcut> getShortcuts() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT Shortcut.ID, Name, Address, Port, IPv6 FROM " +
                "Shortcut LEFT JOIN ShortcutAddress ON Shortcut.ID=ShortcutAddress.ShortcutID ORDER " +
                "BY Shortcut.ID, ShortcutAddress.ID ASC", new String[]{});
        List<Shortcut> shortcuts = new ArrayList<>();
        if (cursor.moveToFirst()) {
            List<IPPortPair> pairs = new ArrayList<>();
            int id = -1, newID;
            do{
                newID = cursor.getInt(cursor.getColumnIndex("Shortcut.ID"));
                if(id == -1 || newID == id){
                    pairs.add(new IPPortPair(cursor.getString(cursor.getColumnIndex("Address")),
                            cursor.getInt(cursor.getColumnIndex("Port")),
                            cursor.getInt(cursor.getColumnIndex("IPv6")) == 1));
                }else{
                    shortcuts.add(new Shortcut(cursor.getString(cursor.getColumnIndex("Name")), new ArrayList<>(pairs)));
                    pairs.clear();
                }
                id = newID;
            }while(cursor.moveToNext());
        }
        cursor.close();
        return shortcuts;
    }

    public synchronized List<DNSEntry> getDNSEntries(){
        List<DNSEntry> entries = new ArrayList<>();
        try {
            Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM DNSEntries", new String[]{});
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndex("Name"));
                    int shortName = cursor.getColumnIndex("ShortName");
                    entries.add(new DNSEntry(cursor.getInt(cursor.getColumnIndex("ID")), name,
                            cursor.isNull(shortName) ? name : cursor.getString(shortName),
                            cursor.getString(cursor.getColumnIndex("dns1")), cursor.getString(cursor.getColumnIndex("dns2")),
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
