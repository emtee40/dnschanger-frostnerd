package com.frostnerd.dnschanger.util;

import android.content.ContentValues;

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
    private DatabaseHelper helper;

    public QueryLogger(DatabaseHelper databaseHelper){
        this.helper = databaseHelper;
    }

    public void logQuery(String query, boolean ipv6){
        ContentValues values = new ContentValues(2);
        values.put("Host", query);
        values.put("IPv6", ipv6);
        helper.getWritableDatabase().insert("DNSQueries", null, values);
    }
}
