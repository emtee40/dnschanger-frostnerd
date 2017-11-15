package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.annotations.ForeignKey;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.Table;


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
public class DNSRuleImport extends Entity{
    @Named(name = "Filename")
    private String filename;
    @Named(name = "Time")
    private long time;
    @ForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
    @Named(name = "FirstInsert")
    private DNSRule firstInsert;
    @ForeignKey(referencedEntity = DNSRule.class, referencedField = "rowid")
    @Named(name = "LastInsert")
    private DNSRule lastInsert;

    public DNSRuleImport(String filename, long time, DNSRule firstInsert, DNSRule lastInsert) {
        this.filename = filename;
        this.time = time;
        this.firstInsert = firstInsert;
        this.lastInsert = lastInsert;
    }

    public DNSRuleImport(){

    }

    public String getFilename() {
        return filename;
    }

    public long getTime() {
        return time;
    }

    public DNSRule getFirstInsert() {
        return firstInsert;
    }

    public DNSRule getLastInsert() {
        return lastInsert;
    }
}
