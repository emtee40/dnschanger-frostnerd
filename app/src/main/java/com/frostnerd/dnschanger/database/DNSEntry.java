package com.frostnerd.dnschanger.database;

import android.support.annotation.NonNull;

public class DNSEntry implements Comparable<DNSEntry> {
    private String name, dns1, dns2, dns1V6, dns2V6, description, shortName;
    private int ID;
    private boolean customEntry;

    public DNSEntry(int id, String name, String shortName, String dns1, String dns2, String dns1V6, String dns2V6, String description, boolean customEntry) {
        this.name = name;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1V6 = dns1V6;
        this.dns2V6 = dns2V6;
        this.ID = id;
        this.description = description;
        this.customEntry = customEntry;
        this.shortName = shortName;
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

    public String getShortName() {
        return shortName;
    }

    public int getID() {
        return ID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDns1(String dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(String dns2) {
        this.dns2 = dns2;
    }

    public void setDns1V6(String dns1V6) {
        this.dns1V6 = dns1V6;
    }

    public void setDns2V6(String dns2V6) {
        this.dns2V6 = dns2V6;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public int compareTo(@NonNull DNSEntry o) {
        return name.compareTo(o.name);
    }

    public boolean hasIP(String ip){
        if(ip == null || ip.equals(""))return false;
        return ip.equals(dns1) || ip.equals(dns2) || ip.equalsIgnoreCase(dns1V6) || ip.equalsIgnoreCase(dns2V6);
    }
}