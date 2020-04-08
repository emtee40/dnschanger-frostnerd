package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.RowID;
import com.frostnerd.database.orm.annotations.Serialized;
import com.frostnerd.database.orm.annotations.Table;
import com.frostnerd.dnschanger.database.serializers.IPPortSerializer;

import java.util.HashSet;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
@Table(name = "DNSTLSConfiguration")
public class DNSTLSConfiguration extends MultitonEntity {
    @Named(name = "id")
    @RowID
    private long ID;
    @Named(name = "port")
    private int port;
    @Named(name = "host")
    @Nullable
    private String hostName;
    @Serialized(scope = Serialized.Scope.INNER, using = IPPortSerializer.class)
    @NonNull
    @Named(name = "affected_servers")
    private HashSet<IPPortPair> affectedServers;

    public DNSTLSConfiguration() {
        
    }

    public DNSTLSConfiguration(int port, @NonNull HashSet<IPPortPair> affectedServers) {
        this(port, affectedServers, null);
    }

    public DNSTLSConfiguration(int port, @NonNull HashSet<IPPortPair> affectedServers, @Nullable String hostName) {
        this.port = port;
        this.hostName = hostName;
        this.affectedServers = affectedServers;
    }

    public int getPort() {
        return port;
    }

    public long getID() {
        return ID;
    }

    public String getHostName() {
        return hostName;
    }

    @NonNull
    public Set<IPPortPair> getAffectedServers() {
        return affectedServers;
    }

    @Override
    public String toString() {
        return "DNSTLSConfiguration{" +
                "ID=" + ID +
                ", port=" + port +
                ", hostName='" + hostName + '\'' +
                ", affectedServers=" + affectedServers +
                '}';
    }
}
