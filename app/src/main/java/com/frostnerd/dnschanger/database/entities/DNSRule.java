package com.frostnerd.dnschanger.database.entities;

import android.support.annotation.NonNull;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Default;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.PrimaryKey;
import com.frostnerd.database.orm.annotations.RowID;
import com.frostnerd.database.orm.annotations.Table;


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
public class DNSRule extends MultitonEntity {
    @PrimaryKey
    @Named(name = "Host")
    @NonNull
    @NotNull
    private String host;
    @NotNull
    @Named(name = "Target")
    @NonNull
    private String target;
    @PrimaryKey
    @Named(name = "Ipv6")
    @Default(defaultValue = "0")
    private boolean ipv6 = false;
    @Named(name = "Wildcard")
    @Default(defaultValue = "0")
    private boolean wildcard = false;
    @RowID
    private long rowid;

    public DNSRule(){

    }

    public DNSRule(@NonNull String host, @NonNull String target, boolean ipv6, boolean wildcard) {
        this.host = host;
        this.target = target;
        this.ipv6 = ipv6;
        this.wildcard = wildcard;
    }

    @NonNull
    public String getHost() {
        return host;
    }

    @NonNull
    public String getTarget() {
        return target;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setTarget(@NonNull String target) {
        this.target = target;
    }

    public long getRowid() {
        return rowid;
    }

    @Override
    public String toString() {
        return "DNSRule{" +
                "host='" + host + '\'' +
                ", target='" + target + '\'' +
                ", ipv6=" + ipv6 +
                ", wildcard=" + wildcard +
                ", rowid=" + rowid +
                '}';
    }
}
