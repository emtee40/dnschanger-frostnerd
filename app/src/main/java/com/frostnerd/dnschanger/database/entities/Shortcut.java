package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.NotNull;
import com.frostnerd.database.orm.annotations.Table;

import java.io.Serializable;

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