package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.dnschanger.util.Util;

import java.io.Serializable;
import java.util.ArrayList;

public class Shortcut implements Serializable {
    private ArrayList<IPPortPair> servers;
    private final String name;

    public Shortcut(String name, ArrayList<IPPortPair> servers) {
        this.name = name;
        this.servers = servers;
    }

    public ArrayList<IPPortPair> getServers() {
        return servers;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return Util.serializableToString(this);
    }

    public static Shortcut fromString(String s) {
        return Util.serializableFromString(s);
    }
}