package com.frostnerd.dnschanger.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DatabaseHelper extends com.frostnerd.utils.database.DatabaseHelper {
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 2;
    private Context context;
    private static final Set<Class<? extends Entity>> entities = new HashSet<Class<? extends Entity>>(){{
        add(DNSEntry.class);
        add(DNSQuery.class);
        add(DNSRule.class);
        add(DNSRuleImport.class);
        add(IPPortPair.class);
        add(Shortcut.class);
    }};

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION, entities);
        this.context = context;
        getSuperWriteableDatabase();
    }

    @Override
    public void onAfterCreate(SQLiteDatabase db) {
        for(DNSEntry entry: DNSEntry.defaultDNSEntries.keySet()){
            insert(entry);
        }
    }

    @Override
    public void onBeforeCreate(SQLiteDatabase db) {

    }

    @Override
    public void onBeforeUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println(">>>>>>>>>>>>>>UPDATING DATABASE (" + oldVersion + " TO " + newVersion + ")");
        for(int i = 0; i <= 10; i++) System.out.println(i + "-<-<->><<" );
        if(oldVersion <= 1){
            Cursor cursor = db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6 FROM Shortcuts", null);
            List<Shortcut> shortcuts = new ArrayList<>();
            List<DNSEntry> entries = new ArrayList<>();
            if(cursor.moveToFirst()){
                do{
                    shortcuts.add(new Shortcut(cursor.getString(0),
                            IPPortPair.wrap(cursor.getString(1), 53),
                            IPPortPair.wrap(cursor.getString(2), 53),
                            IPPortPair.wrap(cursor.getString(3), 53),
                            IPPortPair.wrap(cursor.getString(4), 53)));
                }while(cursor.moveToNext());
            }
            cursor.close();
            cursor = db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6, description, CustomEntry FROM DNSEntries", null);
            if(cursor.moveToFirst()){
                do{
                    DNSEntry found = DNSEntry.findDefaultEntryByLongName(cursor.getString(0));
                    if(found == null)entries.add(new DNSEntry(cursor.getString(0), cursor.getString(0),
                            IPPortPair.wrap(cursor.getString(1), 53),
                            IPPortPair.wrap(cursor.getString(2), 53),
                            IPPortPair.wrap(cursor.getString(3), 53),
                            IPPortPair.wrap(cursor.getString(4), 53),
                            cursor.getString(5),
                            cursor.getInt(6) == 1));
                }while(cursor.moveToNext());
            }
            cursor.close();
            db.execSQL("DROP TABLE IF EXISTS Shortcuts");
            db.execSQL("DROP TABLE IF EXISTS DNSEntries");
            onCreate(db);
            for(DNSEntry entry: entries)insert(entry);
            for(Shortcut shortcut: shortcuts)insert(shortcut);
        }
    }

    @Override
    public void onAfterUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version;
        for(DNSEntry entry: DNSEntry.defaultDNSEntries.keySet()){
            version = DNSEntry.defaultDNSEntries.get(entry);
            if(version > oldVersion && version <= newVersion)insert(entry);
        }
    }

    @Override
    public synchronized void close() {
        context = null;
        super.close();
    }

    public Context getContext() {
        return context;
    }

    public boolean dnsRuleExists(String host){
        return this.rowExists(DNSRule.class, WhereCondition.equal(findColumn(DNSRule.class, "host"), host));
    }

    public boolean dnsRuleExists(String host, boolean ipv6){
        return this.rowExists(DNSRule.class,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0"));
    }

    public DNSRule getDNSRule(String host, boolean ipv6){
        return getSQLHandler(DNSRule.class).selectFirstRow(this, false,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0"));
    }

    public boolean deleteDNSRule(String host, boolean ipv6){
        return delete(DNSRule.class,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0")) != 0;
    }

    public int editDNSRule(String host, boolean ipv6, String newTarget){
        DNSRule rule = getDNSRule(host, ipv6);
        rule.setTarget(newTarget);
        return update(rule);
    }

    public void createDNSRule(String host, String target, boolean ipv6, boolean wildcard){
        insert(new DNSRule(host, target, ipv6, wildcard));
    }


    public void createShortcut(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1v6, IPPortPair dns2v6){
        insert(new Shortcut(name, dns1, dns2, dns1v6, dns2v6));
    }
}
