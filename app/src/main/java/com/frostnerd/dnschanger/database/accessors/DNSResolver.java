package com.frostnerd.dnschanger.database.accessors;

import android.content.Context;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.database.orm.parser.Column;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.OrderOption;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DNSResolver {
    private DatabaseHelper db;
    private int wildcardCount;
    private ParsedEntity<DNSRule> ruleEntity;
    private Column<DNSRule> targetColumn, hostColumn, ipv6Column, wildcardColumn;

    public DNSResolver(Context context) {
        db = Util.getDBHelper(context);
        ruleEntity = db.getSQLHandler(DNSRule.class);
        targetColumn = ruleEntity.getTable().findColumn("target");
        hostColumn = ruleEntity.getTable().findColumn("host");
        ipv6Column = ruleEntity.getTable().findColumn("ipv6");
        wildcardColumn = ruleEntity.getTable().findColumn("wildcard");
        wildcardCount = ruleEntity.getCount(db, WhereCondition.equal(wildcardColumn, "1"));
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
        return ruleEntity.selectFirstRowValue(db, targetColumn, false,
                WhereCondition.equal(ipv6Column, ipv6 ? "1" : "0"),
                WhereCondition.equal(wildcardColumn, "0"),
                WhereCondition.equal(hostColumn, host));
    }

    public String resolveWildcard(String host, boolean ipv6, boolean matchFirst) {
        if(matchFirst){
            return ruleEntity.selectFirstRowValue(db, targetColumn, false,
                    WhereCondition.equal(ipv6Column, ipv6 ? "1" : "0"),
                    WhereCondition.equal(wildcardColumn, "0"),
                    WhereCondition.equal(hostColumn, host));
        }else{
            return ruleEntity.selectFirstRowValue(db, targetColumn, false,
                    WhereCondition.equal(ipv6Column, ipv6 ? "1" : "0"),
                    WhereCondition.equal(wildcardColumn, "0"),
                    WhereCondition.equal(hostColumn, host),
                    new OrderOption(OrderOption.Option.RANDOM));
        }
    }
}
