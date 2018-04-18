package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.RowID;
import com.frostnerd.utils.database.orm.annotations.Table;

import lombok.NoArgsConstructor;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
@NoArgsConstructor
@Table(name = "DNSTLSConfiguration")
public class DNSTLSConfiguration extends MultitonEntity {
    @Named(name = "id")
    @RowID
    private long ID;
    @Named(name = "port")
    private int port;
    @Named(name = "host")
    private String hostName;

    public DNSTLSConfiguration(int port, String hostName) {
        this.port = port;
        this.hostName = hostName;
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
}
