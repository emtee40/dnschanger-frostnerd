package com.frostnerd.dnschanger.database.accessors;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class QueryLogger {
    private final DatabaseHelper helper;
    private final String insertStatement;

    public QueryLogger(DatabaseHelper databaseHelper){
        this.helper = databaseHelper;
        String host = databaseHelper.findColumn(DNSQuery.class, "host").getColumnName(),
                ipv6 = databaseHelper.findColumn(DNSQuery.class, "ipv6").getColumnName(),
                time = databaseHelper.findColumn(DNSQuery.class, "time").getColumnName();
        insertStatement = "INSERT INTO " + databaseHelper.getTableName(DNSQuery.class) + "(" + host +
                "," + ipv6 + "," + time + ")VALUES(?,?,?)";
    }

    public void logQuery(String query, boolean ipv6){
        helper.getWritableDatabase().execSQL(insertStatement, new Object[]{query, ipv6, System.currentTimeMillis()});
    }
}
