package com.frostnerd.dnschanger.database.entities;

import android.support.annotation.NonNull;

import com.frostnerd.dnschanger.database.serializers.IPPortSerializer;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.annotations.AutoIncrement;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.NotNull;
import com.frostnerd.utils.database.orm.annotations.PrimaryKey;
import com.frostnerd.utils.database.orm.annotations.Serialized;
import com.frostnerd.utils.database.orm.annotations.Table;
import com.frostnerd.utils.database.orm.annotations.Unique;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Table(name = "DNSEntry")
public class DNSEntry extends Entity implements Comparable<DNSEntry>{
    @Serialized(using = IPPortSerializer.class)
    @Named(name = "dns1")
    @NotNull
    private IPPortPair dns1;

    @Named(name = "dns2")
    @Serialized(using = IPPortSerializer.class)
    private IPPortPair dns2;

    @Named(name = "dns1v6")
    @Serialized(using = IPPortSerializer.class)
    @NotNull
    private IPPortPair dns1V6;

    @Named(name = "dns2v6")
    @Serialized(using = IPPortSerializer.class)
    private IPPortPair dns2V6;

    @Named(name = "name")
    @NotNull
    @Unique
    private String name;

    @Named(name = "description")
    private String description;

    @Named(name = "shortname")
    private String shortName;

    @Named(name = "customentry")
    private boolean customEntry;

    @PrimaryKey
    @AutoIncrement
    @Named(name = "id")
    private int ID;

    public static final List<DNSEntry> defaultDNSEntries = new ArrayList<>();
    public static final HashMap<String, DNSEntry> additionalDefaultEntries = new HashMap<>();
    static {
        defaultDNSEntries.add(DNSEntry.constructSimple("Google", "Google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple( "OpenDNS", "OpenDNS", "208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Level3", "Level3", "209.244.0.3", "209.244.0.4", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("FreeDNS", "FreeDNS", "37.235.1.174", "37.235.1.177", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Yandex", "Yandex", "77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Verisign", "Verisign", "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Alternate", "Alternate", "198.101.242.72", "23.253.163.53", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Norton Connectsafe - Security", "Norton Connectsafe", "199.85.126.10", "199.85.127.10", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Norton Connectsafe - Security + Pornography" , "Norton Connectsafe", "199.85.126.20", "199.85.127.20", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Norton Connectsafe - Security + Pornography + Other", "Norton Connectsafe", "199.85.126.30", "199.85.127.30", "", "", "",false));
        defaultDNSEntries.add(DNSEntry.constructSimple("Quad9", "Quad9", "9.9.9.9", "", "2620:fe::fe", "", "", false));
        Collections.sort(defaultDNSEntries);

        additionalDefaultEntries.put("unblockr", DNSEntry.constructSimple("Unblockr", "Unblockr", "178.62.57.141", "139.162.231.18", "", "", "Non-public DNS server for kodi. Visit unblockr.net for more information.",false));
    }

    public DNSEntry(String name, String shortName, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6, String description, boolean customEntry) {
        this.name = name;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1V6 = dns1V6;
        this.dns2V6 = dns2V6;
        this.description = description;
        this.customEntry = customEntry;
        this.shortName = shortName;
    }

    public DNSEntry(){

    }

    public static DNSEntry findDefaultEntryByLongName(String name){
        for(DNSEntry entry: defaultDNSEntries)if(entry.getName().equalsIgnoreCase(name))return entry;
        for(DNSEntry entry: additionalDefaultEntries.values())if(entry.getName().equalsIgnoreCase(name))return entry;
        return null;
    }

    public static DNSEntry constructSimple(String name, String shortName, String dns1, String dns2, String dns1V6, String dns2V6, String description, boolean customEntry){
        return new DNSEntry(name, shortName, IPPortPair.wrap(dns1, 53),IPPortPair.wrap(dns2, 53),
                IPPortPair.wrap(dns1V6, 53),IPPortPair.wrap(dns2V6, 53), description, customEntry);
    }

    public String getName() {
        return name;
    }

    public IPPortPair getDns1() {
        return dns1;
    }

    public IPPortPair getDns2() {
        return dns2;
    }

    public IPPortPair getDns1V6() {
        return dns1V6;
    }

    public IPPortPair getDns2V6() {
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

    public void setDns1(IPPortPair dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(IPPortPair dns2) {
        this.dns2 = dns2;
    }

    public void setDns1V6(IPPortPair dns1V6) {
        this.dns1V6 = dns1V6;
    }

    public void setDns2V6(IPPortPair dns2V6) {
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
        return entryAddressMatches(ip, dns1) || entryAddressMatches(ip, dns2) ||
                entryAddressMatches(ip, dns1V6) || entryAddressMatches(ip, dns2V6);
    }

    @Override
    public String toString() {
        return "DNSEntry{" +
                "dns1=" + dns1 +
                ", dns2=" + dns2 +
                ", dns1V6=" + dns1V6 +
                ", dns2V6=" + dns2V6 +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", shortName='" + shortName + '\'' +
                ", customEntry=" + customEntry +
                ", ID=" + ID +
                '}';
    }

    private boolean entryAddressMatches(String ip, IPPortPair pair){
        return ip != null && pair != null && ip.equals(pair.getAddress());
    }
}