package com.frostnerd.dnschanger.database.entities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.ForeignKey;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.Table;
import com.frostnerd.dnschanger.database.DatabaseHelper;

import java.text.DateFormat;


/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
@Table(name = "DNSRuleImport")
public class DNSRuleImport extends MultitonEntity {
    @Named(name = "Filename")
    @NonNull
    @NotNull
    private String filename;
    @Named(name = "Time")
    private long time;
    @ForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
    @Named(name = "FirstInsert")
    private long firstInsert;
    @ForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
    @Named(name = "LastInsert")
    private long lastInsert;

    public DNSRuleImport(@NonNull String filename, long time, long firstInsertRowID, long lastInsertRowID) {
        this.filename = filename;
        this.time = time;
        this.firstInsert = firstInsertRowID;
        this.lastInsert = lastInsertRowID;
    }

    public DNSRuleImport(){

    }

    @NonNull
    public String getFilename() {
        return filename;
    }

    public long getTime() {
        return time;
    }

    @Nullable
    public DNSRule getFirstInsert(@NonNull DatabaseHelper databaseHelper) {
        return databaseHelper.getByRowID(DNSRule.class, firstInsert);
    }

    @Nullable
    public DNSRule getLastInsert(@NonNull DatabaseHelper databaseHelper) {
        return databaseHelper.getByRowID(DNSRule.class, firstInsert);
    }

    public long getFirstInsert() {
        return firstInsert;
    }

    public long getLastInsert() {
        return lastInsert;
    }

    private static final DateFormat TIME_FORMATTER = DateFormat.getDateTimeInstance();

    @Override
    public String toString() {
        return filename + " (" + TIME_FORMATTER.format(time) + ", " + (lastInsert-firstInsert) + " total)";
    }
}
