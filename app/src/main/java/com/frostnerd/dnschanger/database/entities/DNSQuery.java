package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.PrimaryKey;
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
@Table(name = "DNSQuery")
public class DNSQuery extends MultitonEntity {
    @PrimaryKey
    @Named(name = "Host")
    private String host;
    @Named(name = "Ipv6")
    private boolean ipv6;
    @PrimaryKey
    @Named(name = "Time")
    private long time;

    public DNSQuery(String host, boolean ipv6, long time) {
        this.host = host;
        this.ipv6 = ipv6;
        this.time = time;
    }

    public DNSQuery(){

    }

    public String getHost() {
        return host;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public long getTime() {
        return time;
    }
}
