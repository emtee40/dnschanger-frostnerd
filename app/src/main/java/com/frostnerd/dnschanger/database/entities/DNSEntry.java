package com.frostnerd.dnschanger.database.entities;

import android.support.annotation.NonNull;

import com.frostnerd.dnschanger.database.serializers.IPPortSerializer;
import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.NotNull;
import com.frostnerd.utils.database.orm.annotations.RowID;
import com.frostnerd.utils.database.orm.annotations.Serialized;
import com.frostnerd.utils.database.orm.annotations.Table;
import com.frostnerd.utils.database.orm.annotations.Unique;

import java.util.Comparator;
import java.util.TreeMap;

@Table(name = "DNSEntry")
public class DNSEntry extends MultitonEntity implements Comparable<DNSEntry>{
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

    @Named(name = "id")
    @RowID
    private long ID;

    public static final TreeMap<DNSEntry, Integer> defaultDNSEntries = new TreeMap<>(new Comparator<DNSEntry>() {
        @Override
        public int compare(DNSEntry dnsEntry, DNSEntry t1) {
            return dnsEntry.compareTo(t1);
        }
    });
    static {
        defaultDNSEntries.put(DNSEntry.constructSimple("Google", "Google", "8.8.8.8",
                "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple( "OpenDNS", "OpenDNS", "208.67.222.222",
                "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Level3", "Level3", "209.244.0.3",
                "209.244.0.4", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("FreeDNS", "FreeDNS", "37.235.1.174",
                "37.235.1.177", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("DNS.Watch", "DNS.Watch",
                "84.200.69.80", "84.200.70.40", "2001:1608:10:25::1c04:b12f", "2001:1608:10:25::9249:d69b", "", false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("Cloudflare", "Cloudflare",
                "1.1.1.1", "1.0.0.1", "2606:4700:4700::1111", "2606:4700:4700::1001", "", false ), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("Yandex", "Yandex", "77.88.8.8",
                "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Verisign", "Verisign", "64.6.64.6",
                "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Alternate", "Alternate", "198.101.242.72",
                "23.253.163.53", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Norton Connectsafe - Security", "Norton Connectsafe",
                "199.85.126.10", "199.85.127.10", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Norton Connectsafe - Security + Pornography" ,
                "Norton Connectsafe", "199.85.126.20", "199.85.127.20", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Norton Connectsafe - Security + Pornography + Other",
                "Norton Connectsafe", "199.85.126.30", "199.85.127.30", "", "", "",false), 0);
        defaultDNSEntries.put(DNSEntry.constructSimple("Quad9", "Quad9", "9.9.9.9", "",
                "2620:fe::fe", "", "", false), 2);
        defaultDNSEntries.put(DNSEntry.constructSimple("Quad9 secondary", "Quad9 secondary", "9.9.9.10", "",
                "2620:fe::10", "", "", false), 2);
        defaultDNSEntries.put(DNSEntry.constructSimple("Comodo secure", "Comodo",
                "8.26.56.26", "8.20.247.20", "", "", "", false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("Unblockr US", "Unblockr", "138.68.29.183",
                "139.162.231.18", "", "",
                "Non-public DNS server for kodi. Visit unblockr.net for more information.",false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("Unblockr UK", "Unblockr", "178.62.57.141",
                "139.162.231.18", "", "",
                "Non-public DNS server for kodi. Visit unblockr.net for more information.",false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("SafeDNS", "SafeDNS",
                "195.46.39.39", "195.46.39.40", "", "", "Non-public (paid) DNS server with anti-porn filtering [safedns.com]", false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("OpenNIC (Anycast)", "OpenNIC",
                "185.121.177.177", "169.239.202.202", "2a05:dfc7:5::53", "2a05:dfc7:5::5353",
                "Anycast servers of the OpenNIC project. For faster speeds use a server located closer to you.", false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("Verisign", "Verisign",
                "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1b::2:2", "", false),  3);
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
        for(DNSEntry entry: defaultDNSEntries.keySet())if(entry.getName().equalsIgnoreCase(name))return entry;
        return null;
    }

    private static DNSEntry constructSimple(String name, String shortName, String dns1, String dns2, String dns1V6, String dns2V6, String description, boolean customEntry){
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

    public long getID() {
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

    public boolean hasIP(String ip) {
        return !(ip == null || ip.equals("")) && (entryAddressMatches(ip, dns1) || entryAddressMatches(ip, dns2) || entryAddressMatches(ip, dns1V6) || entryAddressMatches(ip, dns2V6));
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