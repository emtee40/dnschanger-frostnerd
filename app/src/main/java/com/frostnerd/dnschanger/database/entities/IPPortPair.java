package com.frostnerd.dnschanger.database.entities;

import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.database.orm.Entity;
import com.frostnerd.utils.database.orm.MultitonEntity;
import com.frostnerd.utils.database.orm.annotations.Named;
import com.frostnerd.utils.database.orm.annotations.RowID;
import com.frostnerd.utils.database.orm.annotations.Table;

import java.io.Serializable;

@Table(name = "IPPortPair")
public class IPPortPair extends MultitonEntity implements Serializable{
    @Named(name = "IP")
    private String ip;
    @Named(name = "Port")
    private int port;
    @Named(name = "Ipv6")
    private boolean ipv6;
    public static final IPPortPair EMPTY = new IPPortPair("", -1, false);

    @RowID
    private long id;

    public IPPortPair(){

    }

    public IPPortPair(String ip, int port, boolean IPv6) {
        this.ip = ip;
        this.port = port;
        this.ipv6 = IPv6;
    }

    public IPPortPair(IPPortPair pair){
        this.ip = pair.getAddress();
        this.port = pair.getPort();
        this.ipv6 = pair.isIpv6();
    }

    public static IPPortPair wrap(String s){
        return Util.validateInput(s, s.contains("[") || s.matches("[a-fA-F0-9:]+"), true, true);
    }

    public static IPPortPair wrap(String s, int defPort){
        return Util.validateInput(s, s.contains("[") || s.matches("[a-fA-F0-9:]+"), true, defPort);
    }

    public String getAddress() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIpv6(boolean ipv6) {
        this.ipv6 = ipv6;
    }

    @Override
    public String toString() {
       return toString(true);
    }

    public String toString(boolean port){
        if(isEmpty())return "";
        if(port)return ipv6 ? "[" + getAddress() + "]:" + getPort() : getAddress() + ":" + getPort();
        else return getAddress();
    }

    public String formatForTextfield(boolean customPorts){
        if(ip.equals(""))return "";
        if(ipv6){
            return customPorts ? "[" + ip + "]:" + port : ip;
        }else{
            return customPorts ? ip + ":" + port : ip;
        }
    }

    public boolean isEmpty(){
        return getAddress().equals("");
    }

}