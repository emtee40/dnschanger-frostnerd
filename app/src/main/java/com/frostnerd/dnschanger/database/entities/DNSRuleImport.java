package com.frostnerd.dnschanger.database.entities;

import java.sql.Timestamp;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class DNSRuleImport {
    private String filename;
    private Timestamp time;
    private int id;

    public DNSRuleImport(String filename, Timestamp time, int id) {
        this.filename = filename;
        this.time = time;
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public Timestamp getTime() {
        return time;
    }

    public int getID() {
        return id;
    }
}
