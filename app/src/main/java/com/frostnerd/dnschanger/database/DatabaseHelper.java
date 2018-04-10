package com.frostnerd.dnschanger.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.utils.database.CursorWithDefaults;
import com.frostnerd.utils.database.MockedContext;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.utils.general.Utils;

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
    public static final String DATABASE_NAME = "data";
    public static final int DATABASE_VERSION = 3;
    @NonNull
    public static final Set<Class<? extends Entity>> entities = new HashSet<Class<? extends Entity>>(){{
        add(DNSEntry.class);
        add(DNSQuery.class);
        add(DNSRule.class);
        add(DNSRuleImport.class);
        add(IPPortPair.class);
        add(Shortcut.class);
    }};
    @Nullable
    private static DatabaseHelper instance;
    @NonNull
    private MockedContext wrappedContext;

    public static DatabaseHelper getInstance(@NonNull Context context){
        return instance == null ? instance = new DatabaseHelper(mock(context)) : instance;
    }

    public static boolean instanceActive(){
        return instance != null;
    }

    private DatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION, entities);
        wrappedContext = (MockedContext) context;
    }

    @Override
    public void onAfterCreate(SQLiteDatabase db) {
        getSQLHandler(DNSEntry.class).insert(this, DNSEntry.defaultDNSEntries.keySet());
    }

    @Override
    public void onBeforeCreate(SQLiteDatabase db) {

    }

    @Override
    public void onBeforeUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion <= 1){
            CursorWithDefaults cursor = CursorWithDefaults.of(db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6 FROM Shortcuts", null));
            List<Shortcut> shortcuts = new ArrayList<>();
            List<DNSEntry> entries = new ArrayList<>();
            if(cursor.moveToFirst()){
                int i = 0;
                do{
                    shortcuts.add(new Shortcut(legacyString(0, cursor, "Name " + i++),
                            IPPortPair.wrap(cursor.getString(1, ""), 53),
                            IPPortPair.wrap(cursor.getString(2 ), 53),
                            IPPortPair.wrap(cursor.getString(3, ""), 53),
                            IPPortPair.wrap(cursor.getString(4), 53)));
                }while(cursor.moveToNext());
            }
            cursor.close();
            cursor = CursorWithDefaults.of(db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6, description, CustomEntry FROM DNSEntries", null));
            if(cursor.moveToFirst()){
                int i = 0;
                do{
                    DNSEntry found = DNSEntry.findDefaultEntryByLongName(cursor.getString(0));
                    if(found == null)entries.add(new DNSEntry(legacyString(0, cursor, "Name " + i++),
                            legacyString(0, cursor,"Name " + i),
                            IPPortPair.wrap(cursor.getString(1, ""), 53),
                            IPPortPair.wrap(cursor.getString(2), 53),
                            IPPortPair.wrap(cursor.getString(3, ""), 53),
                            IPPortPair.wrap(cursor.getString(4), 53),
                            cursor.getString(5, ""),
                            cursor.getInt(6, 0) == 1));
                }while(cursor.moveToNext());
            }
            cursor.close();
            db.execSQL("DROP TABLE IF EXISTS Shortcuts");
            db.execSQL("DROP TABLE IF EXISTS DNSEntries");
            onCreate(db);
            for(DNSEntry entry: entries)insert(entry);
            for(Shortcut shortcut: shortcuts){
                createShortcut(shortcut);
            }
        }
    }

    private String legacyString(@NonNull String column, @NonNull CursorWithDefaults cursor, @NonNull String defaultValue){
        return legacyString(cursor.getColumnIndex(column), cursor, defaultValue);
    }

    private String legacyString(int column, @NonNull CursorWithDefaults cursor, @NonNull String defaultValue){
        String fromDB = cursor.getStringNonEmpty(column, defaultValue);
        fromDB = fromDB.replaceAll("\\r|\\n", "");
        return Utils.notEmptyOrDefault(fromDB, defaultValue);
    }

    @Override
    public void onAfterUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version;
        for(DNSEntry entry: DNSEntry.defaultDNSEntries.keySet()){
            version = DNSEntry.defaultDNSEntries.get(entry);
            if(getCount(DNSEntry.class, WhereCondition.equal("name", entry.getName())) == 0)
                if(version > oldVersion && version <= newVersion)insert(entry);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public synchronized void close() {
        instance = null;
        wrappedContext.destroy(false);
        wrappedContext = null;
        super.close();
    }

    public boolean dnsRuleExists(@NonNull String host){
        return this.rowExists(DNSRule.class, WhereCondition.equal(findColumn(DNSRule.class, "host"), host));
    }

    public boolean dnsRuleExists(@NonNull String host, boolean ipv6){
        return this.rowExists(DNSRule.class,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0"));
    }

    public DNSRule getDNSRule(@NonNull String host, boolean ipv6){
        return getSQLHandler(DNSRule.class).selectFirstRow(this, false,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0"));
    }

    public boolean deleteDNSRule(@NonNull String host, boolean ipv6){
        return delete(DNSRule.class,
                WhereCondition.equal(findColumn(DNSRule.class, "host"), host),
                WhereCondition.equal(findColumn(DNSRule.class, "ipv6"), ipv6 ? "1" : "0")) != 0;
    }

    public int editDNSRule(@NonNull String host, boolean ipv6, @NonNull String newTarget){
        DNSRule rule = getDNSRule(host, ipv6);
        rule.setTarget(newTarget);
        return update(rule);
    }

    public void createDNSRule(@NonNull String host, @NonNull String target, boolean ipv6, boolean wildcard){
        insert(new DNSRule(host, target, ipv6, wildcard));
    }


    public void createShortcut(@NonNull String name, @NonNull IPPortPair dns1, @Nullable IPPortPair dns2,
                               @NonNull IPPortPair dns1v6, @Nullable IPPortPair dns2v6) {
        insert(dns1);
        if (dns2 != null) insert(dns2);
        insert(dns1v6);
        if (dns2v6 != null) insert(dns2v6);
        insert(new Shortcut(name, dns1, dns2, dns1v6, dns2v6));
    }

    private void createShortcut(@NonNull Shortcut shortcut) {
        insert(shortcut.getDns1());
        if (shortcut.getDns2() != null) insert(shortcut.getDns2());
        insert(shortcut.getDns1v6());
        if (shortcut.getDns2v6() != null) insert(shortcut.getDns2v6());
        insert(shortcut);
    }

    @Nullable
    public DNSEntry findMatchingDNSEntry(@NonNull String dnsServer){
        String address = "%" + dnsServer + "%";
        if(address.equals("%%"))return null;
        ParsedEntity<DNSEntry> parsedEntity = getSQLHandler(DNSEntry.class);
        return parsedEntity.selectFirstRow(this, false, WhereCondition.like(parsedEntity.getTable().findColumn("dns1"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns2"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns1v6"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns2v6"), address));
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
