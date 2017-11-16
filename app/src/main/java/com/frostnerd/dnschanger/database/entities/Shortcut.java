package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.annotations.ForeignKey;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.NotNull;
import com.frostnerd.utils.database.orm.annotations.Table;

import java.io.Serializable;
import java.util.ArrayList;

@Table(name = "Shortcut")
public class Shortcut extends Entity implements Serializable {
    @NotNull
    @Named(name = "Dns1")
    private IPPortPair dns1;
    @NotNull
    @Named(name = "Dns1v6")
    private IPPortPair dns1v6;
    @Named(name = "Dns2")
    private IPPortPair dns2;
    @Named(name = "Dns2v6")
    private IPPortPair dns2v6;
    @NotNull
    @Named(name = "Name")
    private String name;

    public Shortcut(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1v6, IPPortPair dns2v6) {
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1v6 = dns1v6;
        this.dns2v6 = dns2v6;
        this.name = name;
    }

    public Shortcut(){

    }

    public IPPortPair getDns1() {
        return dns1;
    }

    public IPPortPair getDns1v6() {
        return dns1v6;
    }

    public IPPortPair getDns2() {
        return dns2;
    }

    public IPPortPair getDns2v6() {
        return dns2v6;
    }

    public String getName() {
        return name;
    }
}