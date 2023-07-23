package com.frostnerd.dnschanger.database.accessors;

import android.content.Context;
import android.database.Cursor;

import com.frostnerd.database.orm.parser.ParsedEntity;
import com.frostnerd.database.orm.parser.columns.Column;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSRule;

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
public class DNSResolver {
    private DatabaseHelper db;
    private final int wildcardCount;
    private final String WILDCARD_QUERY_RANDOM;
    private final String WILDCARD_QUERY_FIRST;
    private final String NON_WILDCARD_QUERY;


    public DNSResolver(Context context) {
        db = DatabaseHelper.getInstance(context);
        ParsedEntity<DNSRule> ruleEntity = db.getSQLHandler(DNSRule.class);
        Column<DNSRule> targetColumn = ruleEntity.getTable().requireColumn("target");
        Column<DNSRule> hostColumn = ruleEntity.getTable().requireColumn("host");
        Column<DNSRule> ipv6Column = ruleEntity.getTable().requireColumn("ipv6");
        Column<DNSRule> wildcardColumn = ruleEntity.getTable().requireColumn("wildcard");
        wildcardCount = ruleEntity.getCount(db, WhereCondition.equal(wildcardColumn, "1"));
        WILDCARD_QUERY_RANDOM = "SELECT " + targetColumn.getColumnName() + " FROM " + ruleEntity.getTableName() +
                " WHERE " + ipv6Column.getColumnName() + "" + "=? AND " + wildcardColumn.getColumnName()
                + "=1 AND ? REGEXP " + hostColumn.getColumnName() + " ORDER BY RANDOM() LIMIT 1";
        WILDCARD_QUERY_FIRST = "SELECT " + targetColumn.getColumnName() + " FROM " + ruleEntity.getTableName() +
                " WHERE " + ipv6Column.getColumnName() + "" + "=? AND " + wildcardColumn.getColumnName()
                + "=1 AND ? REGEXP " + hostColumn.getColumnName() + " LIMIT 1";
        NON_WILDCARD_QUERY = "SELECT " + targetColumn.getColumnName() + " FROM " + ruleEntity.getTableName() +
                " WHERE " + hostColumn.getColumnName() + "=? AND " + ipv6Column.getColumnName() + "=? AND " +
                wildcardColumn.getColumnName() + "=0";
    }

    public void destroy(){
        db = null;
    }

    public String resolve(String host, boolean ipv6) {
        return resolve(host, ipv6, true);
    }

    public String resolve(String host, boolean ipv6, boolean allowWildcard) {
        String res;
        res = resolveNonWildcard(host, ipv6);
        if (res == null && allowWildcard && wildcardCount != 0) {
            res = resolveWildcard(host, ipv6, false);
        }
        return res;
    }

    private String resolveNonWildcard(String host, boolean ipv6) {
        String result = null;
        Cursor cursor = db.getReadableDatabase().rawQuery(NON_WILDCARD_QUERY,
                new String[]{host, ipv6 ? "1" : "0"});
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    private String resolveWildcard(String host, boolean ipv6, boolean matchFirst) {
        String result = null;
        Cursor cursor = db.getReadableDatabase().rawQuery(matchFirst ? WILDCARD_QUERY_FIRST : WILDCARD_QUERY_RANDOM,
                new String[]{ipv6 ? "1" : "0", host});
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }
}
