package com.frostnerd.dnschanger.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.database.CursorWithDefaults;
import com.frostnerd.database.orm.Entity;
import com.frostnerd.database.orm.parser.ParsedEntity;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.DNSTLSConfiguration;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.general.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
public class DatabaseHelper extends com.frostnerd.database.DatabaseHelper {
    public static final String DATABASE_NAME = "data";
    public static final int DATABASE_VERSION = 5;
    @NonNull
    public static final Set<Class<? extends Entity>> entities = new HashSet<Class<? extends Entity>>(){{
        add(DNSEntry.class);
        add(DNSQuery.class);
        add(DNSRule.class);
        add(DNSRuleImport.class);
        add(IPPortPair.class);
        add(Shortcut.class);
        add(DNSTLSConfiguration.class);
    }};
    @Nullable
    private static DatabaseHelper instance;

    public static DatabaseHelper getInstance(@NonNull Context context){
        if(instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, DATABASE_VERSION, entities);
        upgradeDnsQuery(getWritableDatabase());
    }

    @Override
    public void onAfterCreate(SQLiteDatabase db) {
        getSQLHandler(DNSEntry.class).insert(this, DNSEntry.defaultDNSEntries.keySet());
    }

    @Override
    public void onBeforeCreate(SQLiteDatabase db) {

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion != 4 && oldVersion != 3) super.onUpgrade(db, oldVersion, newVersion);
    }

    private void upgradeDnsQuery(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM DNSQuery LIMIT 1", null);
            boolean columnFound = false;
            for(String s: cursor.getColumnNames()) {
                if(s.equalsIgnoreCase("upstreamanswer")) {
                    columnFound = true;
                    break;
                }
            }
            if(!columnFound) {
                db.execSQL("ALTER TABLE DNSQuery ADD COLUMN UpstreamAnswer TEXT");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onBeforeUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        System.out.println("Updating from " + oldVersion + " to " + newVersion);
        if(oldVersion <= 1){
            List<Shortcut> shortcuts = new ArrayList<>();
            List<DNSEntry> entries = new ArrayList<>();
            if(tableExists("Shortcuts")){
                CursorWithDefaults cursor = CursorWithDefaults.of(db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6 FROM Shortcuts", null));
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
            }
            if(tableExists("DNSEntries")){
                CursorWithDefaults cursor = CursorWithDefaults.of(db.rawQuery("SELECT Name, dns1, dns2, dns1v6, dns2v6, description, CustomEntry FROM DNSEntries", null));
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
            }
            for(String s: getTableNames(db)){
                if(!s.equalsIgnoreCase("sqlite_sequence"))
                db.execSQL("DROP TABLE IF EXISTS " + s);
            }
            onCreate(db);
            for(DNSEntry entry: entries) insert(entry);
            for(Shortcut shortcut: shortcuts) createShortcut(shortcut);
        }else if(oldVersion <= 3) {
            getSQLHandler(DNSTLSConfiguration.class).createTable(db);
            db.execSQL("DROP TABLE IF EXISTS " + getTableName(DNSRuleImport.class));
            getSQLHandler(DNSRuleImport.class).createTable(db);
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
        if(oldVersion != 4) {
            int version;
            for(Map.Entry<DNSEntry, Integer> entry: DNSEntry.defaultDNSEntries.entrySet()){
                version = entry.getValue();
                if(getCount(DNSEntry.class, WhereCondition.equal("name", entry.getKey().getName())) == 0)
                    if(version > oldVersion && version <= newVersion)insert(entry.getKey());
            }
        }
    }

    public boolean dnsEntryExists(String name){
        return select(DNSEntry.class, WhereCondition.equal("name", name)).size() != 0;
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

    @NonNull
    public Set<DNSEntry> findMatchingDNSEntries(@NonNull String dnsServer) {
        String address = "%" + dnsServer + "%";
        if (address.equals("%%")) return new HashSet<>();
        ParsedEntity<DNSEntry> parsedEntity = getSQLHandler(DNSEntry.class);
        return new HashSet<>(parsedEntity.select(this, false, WhereCondition.like(parsedEntity.getTable().findColumn("dns1"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns2"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns1v6"), address).nextOr(),
                WhereCondition.like(parsedEntity.getTable().findColumn("dns2v6"), address)));
    }

    @Nullable
    public DNSTLSConfiguration findTLSConfiguration(@NonNull DNSEntry entry){
        DNSTLSConfiguration best = null;
        int maxHits = 0;
        for(DNSTLSConfiguration configuration: getAll(DNSTLSConfiguration.class)){
            for(IPPortPair pair: entry.getServers()) {
                int hits = 0;
                for(IPPortPair ip: configuration.getAffectedServers()) {
                    if(pair.getAddress().equalsIgnoreCase(ip.getAddress())) hits++;
                }
                if(hits > maxHits) {
                    maxHits = hits;
                    best = configuration;
                }
            }
        }
        return best;
    }

    public List<DNSEntry> getCustomDNSEntries(){
        return select(DNSEntry.class, WhereCondition.equal("customentry", "1"));
    }

    @Nullable
    public DNSTLSConfiguration findTLSConfiguration(@NonNull IPPortPair pair){
        for(DNSTLSConfiguration configuration: getAll(DNSTLSConfiguration.class)){
            for(IPPortPair ip: configuration.getAffectedServers()) {
                if(ip.getAddress().equalsIgnoreCase(pair.getAddress())) return configuration;
            }
        }
        return null;
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
