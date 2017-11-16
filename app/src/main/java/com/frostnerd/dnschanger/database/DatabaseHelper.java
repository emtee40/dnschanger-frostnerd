package com.frostnerd.dnschanger.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private SQLiteDatabase currentDB;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION, entities);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        super.onCreate(db);
        currentDB = db;
        for(DNSEntry entry: DNSEntry.defaultDNSEntries){
            insert(entry);
        }
        for (DNSEntry entry: DNSEntry.additionalDefaultEntries.values()) {
            insert(entry);
        }
        currentDB = null;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return currentDB == null ? super.getWritableDatabase() : currentDB;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return currentDB == null ? super.getReadableDatabase() : currentDB;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
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
