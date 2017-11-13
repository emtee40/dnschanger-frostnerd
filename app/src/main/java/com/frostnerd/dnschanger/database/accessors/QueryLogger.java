package com.frostnerd.dnschanger.database.accessors;

import android.content.ContentValues;

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
    private DatabaseHelper helper;

    public QueryLogger(DatabaseHelper databaseHelper){
        this.helper = databaseHelper;
    }

    public void logQuery(String query, boolean ipv6){
        helper.insert(new DNSQuery(query, ipv6, System.currentTimeMillis()));
    }
}
