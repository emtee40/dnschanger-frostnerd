package com.frostnerd.dnschanger.database.accessors;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;

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
public class QueryLogger {
    private DatabaseHelper helper;
    private final String insertStatement;
    private static Runnable newQueryLogged;

    public QueryLogger(DatabaseHelper databaseHelper){
        this.helper = databaseHelper;
        String host = databaseHelper.findColumnOrThrow(DNSQuery.class, "host").getColumnName(),
                ipv6 = databaseHelper.findColumnOrThrow(DNSQuery.class, "ipv6").getColumnName(),
                time = databaseHelper.findColumnOrThrow(DNSQuery.class, "time").getColumnName();
        insertStatement = "INSERT INTO " + databaseHelper.getTableName(DNSQuery.class) + "(" + host +
                "," + ipv6 + "," + time + ")VALUES(?,?,?)";
    }

    public void logQuery(String query, boolean ipv6){
        helper.getWritableDatabase().execSQL(insertStatement, new Object[]{query, ipv6, System.currentTimeMillis()});
        if(newQueryLogged != null)newQueryLogged.run();
    }

    public static void setNewQueryLoggedCallback(Runnable runnable){
        newQueryLogged = runnable;
    }

    public void destroy(){
        helper = null;
        newQueryLogged = null;
    }
}
