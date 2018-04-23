package com.frostnerd.dnschanger.database.entities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.serializers.IPPortSerializer;
import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.NotNull;
import com.frostnerd.utils.database.orm.annotations.RowID;
import com.frostnerd.utils.database.orm.annotations.Serialized;
import com.frostnerd.utils.database.orm.annotations.Table;
import com.frostnerd.utils.database.orm.annotations.Unique;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Table(name = "DNSEntry")
public class DNSEntry extends MultitonEntity implements Comparable<DNSEntry>{
    @Serialized(using = IPPortSerializer.class)
    @Named(name = "dns1")
    @NotNull
    @NonNull
    private IPPortPair dns1;

    @Named(name = "dns2")
    @Serialized(using = IPPortSerializer.class)
    @Nullable
    private IPPortPair dns2;

    @Named(name = "dns1v6")
    @Serialized(using = IPPortSerializer.class)
    @NotNull
    @NonNull
    private IPPortPair dns1V6;

    @Named(name = "dns2v6")
    @Serialized(using = IPPortSerializer.class)
    @Nullable
    private IPPortPair dns2V6;

    @Named(name = "name")
    @NotNull
    @Unique
    @NonNull
    private String name;

    @Named(name = "description")
    @NonNull
    private String description;

    @Named(name = "shortname")
    @NonNull
    private String shortName;

    @Named(name = "customentry")
    private boolean customEntry;

    @Named(name = "id")
    @RowID
    private long ID;

    public static final TreeMap<DNSEntry, Integer> defaultDNSEntries = new TreeMap<>();
    public static final HashMap<DNSTLSConfiguration, Integer> defaultTLSConfig = new HashMap<>();
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
        defaultDNSEntries.put(DNSEntry.constructSimple("Quad9", "Quad9", "9.9.9.9", "149.112.112.112",
                "2620:fe::fe", "", "", false), 2);
        defaultDNSEntries.put(DNSEntry.constructSimple("Quad9 Unsecure ", "Quad9 Unsecure", "9.9.9.10", "",
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
        defaultDNSEntries.put(DNSEntry.constructSimple("CleanBrowsing Family Filter", "CleanBrowsing",
                "185.228.168.168", "185.228.168.169", "2a0d:2a00:1::", "2a0d:2a00:2::",
                "Blocks access to all adult sites (including sites like reddit). Google, Bing and Youtube are set to the Safe Mode.", false), 3);
        defaultDNSEntries.put(DNSEntry.constructSimple("CleanBrowsing Adult Filter", "CleanBrowsing",
                "185.228.168.10", "185.228.168.11", "2a0d:2a00:1::1", "2a0d:2a00:2::1",
                "Blocks access to all adult sites. Sites like Reddit are allowed. Google and Bing are set to the Safe Mode.", false), 3);


        DNSEntry cloudflare = findDefaultEntryByLongName("Cloudflare");
        DNSTLSConfiguration cloudflareConfig = new DNSTLSConfiguration(853, new HashSet<>(cloudflare.getServers()), "cloudflare-dns.com");
        defaultTLSConfig.put(cloudflareConfig, 4);
        DNSEntry quad9 = findDefaultEntryByLongName("Quad9");
        DNSTLSConfiguration quad9Config = new DNSTLSConfiguration(853, new HashSet<>(quad9.getServers()), "dns.quad9.net");
        defaultTLSConfig.put(quad9Config, 4);
    }

    public DNSEntry(@NonNull String name, @NonNull String shortName, @NonNull IPPortPair dns1, @Nullable IPPortPair dns2,
                    @NonNull IPPortPair dns1V6, @Nullable IPPortPair dns2V6, @NonNull String description, boolean customEntry) {
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

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public IPPortPair getDns1() {
        return dns1;
    }

    @Nullable
    public IPPortPair getDns2() {
        return dns2;
    }

    @NonNull
    public IPPortPair getDns1V6() {
        return dns1V6;
    }

    @Nullable
    public IPPortPair getDns2V6() {
        return dns2V6;
    }

    public boolean isCustomEntry() {
        return customEntry;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String getShortName() {
        return shortName;
    }

    public long getID() {
        return ID;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setDns1(@NonNull IPPortPair dns1) {
        this.dns1 = dns1;
    }

    public void setDns2(@Nullable IPPortPair dns2) {
        this.dns2 = dns2;
    }

    public void setDns1V6(@NonNull IPPortPair dns1V6) {
        this.dns1V6 = dns1V6;
    }

    public void setDns2V6(@Nullable IPPortPair dns2V6) {
        this.dns2V6 = dns2V6;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public void setShortName(@NonNull String shortName) {
        this.shortName = shortName;
    }

    @Override
    public int compareTo(@NonNull DNSEntry o) {
        return name.compareTo(o.name);
    }

    public boolean hasIP(String ip) {
        return !(ip == null || ip.equals("")) && (entryAddressMatches(ip, dns1) || entryAddressMatches(ip, dns2) || entryAddressMatches(ip, dns1V6) || entryAddressMatches(ip, dns2V6));
    }

    public boolean supportsDNSOverTLS(Context context){
        return DatabaseHelper.getInstance(context).findTLSConfiguration(this) != null;
    }

    private boolean entryAddressMatches(@Nullable String ip, @Nullable IPPortPair pair){
        return ip != null && pair != null && ip.equals(pair.getAddress());
    }

    public Set<IPPortPair> getServers(){
        Set<IPPortPair> servers = new HashSet<>();
        servers.add(dns1);
        servers.add(dns1V6);
        if(dns2 != null && dns2 != IPPortPair.getEmptyPair() && dns2 != IPPortPair.INVALID)
            servers.add(dns2);
        if(dns2V6 != null && dns2V6 != IPPortPair.getEmptyPair() && dns2V6 != IPPortPair.INVALID)
            servers.add(dns2V6);
        return servers;
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
}