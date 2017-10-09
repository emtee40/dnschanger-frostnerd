package com.frostnerd.dnschanger.util;

public class Shortcut {
    private String name, dns1, dns2, dns1v6, dns2v6;

    public Shortcut(String name, String dns1, String dns2, String dns1v6, String dns2v6) {
        this.name = name;
        this.dns1 = dns1;
        this.dns2 = dns2;
        this.dns1v6 = dns1v6;
        this.dns2v6 = dns2v6;
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

    public String getDns1v6() {
        return dns1v6;
    }

    public String getDns2v6() {
        return dns2v6;
    }

    @Override
    public String toString() {
        return dns1 + "<<>>" + dns2 + "<<>>" + dns1v6 + "<<>>" + dns2v6 + "<<>>" + name;
    }

    public static Shortcut fromString(String s) {
        if (s == null || s.equals("") || s.split("<<>>").length < 5) return null;
        String[] splt = s.split("<<>>");
        return new Shortcut(splt[4], splt[0], splt[1], splt[2], splt[3]);
    }
}