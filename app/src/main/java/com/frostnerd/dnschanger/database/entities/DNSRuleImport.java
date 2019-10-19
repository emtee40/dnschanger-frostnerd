package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.DeletableForeignKey;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.Table;
import com.frostnerd.dnschanger.database.DatabaseHelper;

import java.text.DateFormat;


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
@Table(name = "DNSRuleImport")
public class DNSRuleImport extends MultitonEntity {
    @Named(name = "Filename")
    @NonNull
    @NotNull
    private String filename;
    @Named(name = "Time")
    private long time;
    @DeletableForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
    @Named(name = "FirstInsert")
    private long firstInsert;
    @DeletableForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
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
