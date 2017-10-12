package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.database.Cursor;

import java.util.LinkedHashMap;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DNSResolver {
    private static final String WILDCARD_QUERY_RANDOM =
            "SELECT Target FROM DNSRules WHERE IPv6=? AND Wildcard=1 AND ? REGEXP Domain ORDER BY RANDOM() LIMIT 1";
    private static final String WILDCARD_QUERY_FIRST =
            "SELECT Target FROM DNSRules WHERE IPv6=? AND Wildcard=1 AND ? REGEXP Domain LIMIT 1";
    private static final String NON_WILDCARD_QUERY = "SELECT Target FROM DNSRules WHERE IPv6=? AND Domain=? AND Wildcard=?";
    private static final String SUM_WILDCARD_QUERY = "SELECT SUM(Wildcard) FROM DNSRules";
    private DatabaseHelper db;
    private int wildcardCount;

    public DNSResolver(Context context) {
        db = API.getDBHelper(context);
        Cursor cursor = db.getReadableDatabase().rawQuery(SUM_WILDCARD_QUERY, null);
        if (cursor.moveToFirst()) {
            wildcardCount = cursor.getInt(0);
        }
        cursor.close();
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

    public String resolveNonWildcard(String host, boolean ipv6) {
        String result = null;
        Cursor cursor = db.getReadableDatabase().rawQuery(NON_WILDCARD_QUERY,
                new String[]{host, ipv6 ? "1" : "0", "0"});
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    public String resolveWildcard(String host, boolean ipv6, boolean matchFirst) {
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
