package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.PrimaryKey;
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
@Table(name = "DNSQuery")
public class DNSQuery extends MultitonEntity {
    @PrimaryKey
    @Named(name = "Host")
    @NonNull
    @NotNull
    private String host;
    @Named(name = "Ipv6")
    private boolean ipv6;
    @PrimaryKey
    @Named(name = "Time")
    private long time;

    @Nullable
    @Named(name = "UpstreamAnswer")
    private String upstreamAnswer;

    public DNSQuery(@NonNull String host, boolean ipv6, long time) {
        this.host = host;
        this.ipv6 = ipv6;
        this.time = time;
    }

    public DNSQuery(){

    }

    @Nullable
    public String getUpstreamAnswer() {
        return upstreamAnswer;
    }

    public void setUpstreamAnswer(@NonNull String upstreamAnswer) {
        this.upstreamAnswer = upstreamAnswer;
    }

    @NonNull
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
