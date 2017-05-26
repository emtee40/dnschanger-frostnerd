package com.frostnerd.dnschanger.API;

import android.support.annotation.NonNull;

public class DNSEntry implements Comparable<DNSEntry> {
    private String name, dns1, dns2, dns1V6, dns2V6, description;
    private int ID;
    private boolean customEntry;

    public DNSEntry(int id, String name, String dns1, String dns2, String dns1V6, String dns2V6, String description, boolean customEntry) {
        this.name = name;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1V6 = dns1V6;
        this.dns2V6 = dns2V6;
        this.ID = id;
        this.description = description;
        this.customEntry = customEntry;
    }

    public String getName() {
        return name;
    }

    public String getDns1() {
        return dns1;
    }

    public String getDns2() {
        return dns2;
    }

    public String getDns1V6() {
        return dns1V6;
    }

    public String getDns2V6() {
        return dns2V6;
    }

    public boolean isCustomEntry() {
        return customEntry;
    }

    public String getDescription() {
        return description;
    }

    public int getID() {
        return ID;
    }

    @Override
    public int compareTo(@NonNull DNSEntry o) {
        return name.compareTo(o.name);
    }
}