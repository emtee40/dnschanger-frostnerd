package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.annotations.AutoIncrement;
import com.frostnerd.utils.database.orm.annotations.NotNull;
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
@Table(name = "DNSRule")
public class DNSRule extends Entity{
    @PrimaryKey
    private String host;
    @NotNull
    private String target;
    @PrimaryKey
    private boolean ipv6 = false;
    private boolean wildcard = false;

    public DNSRule(){

    }

    public DNSRule(String host, String target, boolean ipv6, boolean wildcard) {
        this.host = host;
        this.target = target;
        this.ipv6 = ipv6;
        this.wildcard = wildcard;
    }

    public String getHost() {
        return host;
    }

    public String getTarget() {
        return target;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
