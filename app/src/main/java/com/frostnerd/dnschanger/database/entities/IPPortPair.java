package com.frostnerd.dnschanger.database.entities;

import androidx.annotation.NonNull;

import com.frostnerd.database.orm.MultitonEntity;
import com.frostnerd.database.orm.annotations.Ignore;
import com.frostnerd.database.orm.annotations.Named;
import com.frostnerd.database.orm.annotations.RowID;
import com.frostnerd.database.orm.annotations.Table;
import com.frostnerd.dnschanger.util.Util;

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
@Table(name = "IPPortPair")
public class IPPortPair extends MultitonEntity implements Serializable{
    @Named(name = "IP")
    @NonNull
    private String ip;
    @Named(name = "Port")
    private int port;
    @Named(name = "Ipv6")
    private boolean ipv6;
    @Ignore
    static final IPPortPair emptyPair = new IPPortPair("", Integer.MIN_VALUE + 1, false);
    @RowID
    private long id;
    public static final IPPortPair INVALID = new IPPortPair("", Integer.MIN_VALUE, false);

    public IPPortPair(){

    }

    public boolean matches(IPPortPair other) {
        if(other == null) return false;
        return other.ipv6 == ipv6 && other.ip.equalsIgnoreCase(ip) && other.port == port;
    }

    public IPPortPair(@NonNull String ip, int port, boolean IPv6) {
        if(!ip.equals("") && (port <= 0 || port > 0xFFFF))
            throw new IllegalArgumentException("Invalid port: " + port + " (Address: " + ip + ")", new Throwable("The invalid port " + port + " was supplied"));
        this.ip = ip;
        this.port = port;
        this.ipv6 = IPv6;
    }

    public IPPortPair(IPPortPair pair){
        this(pair.ip, pair.port, pair.ipv6);
    }

    public static IPPortPair wrap(String s){
        return wrap(s.replace("-1", "53"), 53);
    }

    public static IPPortPair wrap(String s, int defPort){
        return Util.validateInput(s, s.contains("[") || s.matches("[a-fA-F0-9:]+"), true, defPort);
    }

    @NonNull
    public String getAddress() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean isIpv6() {
        return ipv6;
    }

    public void setIp(@NonNull String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        if(port <= 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Invalid port: " + port, new Throwable("The invalid port " + port + " was supplied"));
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
        if(port)return getFormattedWithPort();
        else return getAddress();
    }

    public String formatForTextfield(boolean customPorts){
        if(ip.equals(""))return "";
        return customPorts ? getFormattedWithPort() : ip;
    }

    private String getFormattedWithPort(){
        if(port == -1)return ip;
        if(ipv6){
           return "[" + ip + "]:" + port;
        }else{
           return ip + ":" + port;
        }
    }

    public static IPPortPair getEmptyPair() {
        return emptyPair;
    }

    public boolean isEmpty(){
        return getAddress().equals("") || this == emptyPair;
    }

}