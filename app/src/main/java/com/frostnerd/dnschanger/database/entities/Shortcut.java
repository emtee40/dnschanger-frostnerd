package com.frostnerd.dnschanger.database.entities;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.NotNull;
import com.frostnerd.utils.database.orm.annotations.Table;

import java.io.Serializable;

@Table(name = "Shortcut")
public class Shortcut extends MultitonEntity implements Serializable {
    @NotNull
    @NonNull
    @Named(name = "Dns1")
    private IPPortPair dns1;
    @NotNull
    @NonNull
    @Named(name = "Dns1v6")
    private IPPortPair dns1v6;
    @Named(name = "Dns2")
    private IPPortPair dns2;
    @Named(name = "Dns2v6")
    private IPPortPair dns2v6;
    @NotNull
    @NonNull
    @Named(name = "Name")
    private String name;

    public Shortcut(@NonNull String name, @NonNull IPPortPair dns1, @Nullable IPPortPair dns2, @NonNull IPPortPair dns1v6, @Nullable IPPortPair dns2v6) {
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1v6 = dns1v6;
        this.dns2v6 = dns2v6;
        this.name = name;
    }

    public Shortcut(){

    }

    @NonNull
    public IPPortPair getDns1() {
        return dns1;
    }

    @NonNull
    public IPPortPair getDns1v6() {
        return dns1v6;
    }

    @Nullable
    public IPPortPair getDns2() {
        return dns2;
    }

    @Nullable
    public IPPortPair getDns2v6() {
        return dns2v6;
    }

    @NonNull
    public String getName() {
        return name;
    }
}