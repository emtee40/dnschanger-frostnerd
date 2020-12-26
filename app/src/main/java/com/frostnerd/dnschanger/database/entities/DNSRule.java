package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Default;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.PrimaryKey;
import com.frostnerd.database.orm.annotations.RowID;
import com.frostnerd.database.orm.annotations.Table;


/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
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
