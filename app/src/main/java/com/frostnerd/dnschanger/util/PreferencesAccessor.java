package com.frostnerd.dnschanger.util;

import android.content.Context;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class PreferencesAccessor {

    public static boolean isIPv6Enabled(Context context) {
        return Preferences.getBoolean(context, "setting_ipv6_enabled", false);
    }

    public static boolean isIPv4Enabled(Context context) {
        return Preferences.getBoolean(context, "setting_ipv4_enabled", true);
    }

    public static String getDNS1(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns1", "8.8.8.8") : "";
    }

    public static String getDNS2(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns2", "8.8.4.4") : "";
    }

    public static String getDNS1V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns1-v6", "2001:4860:4860::8888") : "";
    }

    public static String getDNS2V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns2-v6", "2001:4860:4860::8844") : "";
    }

    public static List<String> getAllDNS(final Context context){
        return new ArrayList<String>(){{
            addIfNotEmpty(getDNS1(context));
            addIfNotEmpty(getDNS1V6(context));
            addIfNotEmpty(getDNS2(context));
            addIfNotEmpty(getDNS2V6(context));
        }
            private void addIfNotEmpty(String s){
                if(s != null && !s.equals(""))add(s);
            }
        };
    }

    public static List<IPPortPair> getAllDNSPairs(final Context context, final boolean enabledOnly){
        return new ArrayList<IPPortPair>(){
            private boolean customPorts = Preferences.getBoolean(context, "custom_port", false);
            {
                if(!enabledOnly || isIPv4Enabled(context)){
                    addIfNotEmpty(getDNS1(context), 1);
                    addIfNotEmpty(getDNS2(context), 3);
                }
                if(!enabledOnly || isIPv6Enabled(context)){
                    addIfNotEmpty(getDNS1V6(context), 2);
                    addIfNotEmpty(getDNS2V6(context), 4);
                }
            }
            private void addIfNotEmpty(String s, int id) {
                if (s != null && !s.equals("")) {
                    int port = customPorts ?
                            Preferences.getInteger(context, "port" + (id >= 3 ? "2" : "1") + (id % 2 == 0 ? "v6" : ""), 53)
                            : 53;
                    add(new IPPortPair(s, port, id % 2 == 0));
                }
            }
        };
    }

    public static int getPortForDNS(Context context, String server){
        if(!Preferences.getBoolean(context, "custom_port", false))return 53;
        List<String> dns = getAllDNS(context);
        for(int i = 0; i < dns.size();i++){
            if(dns.get(i).equals(server)){
                return i <= 1 ? Preferences.getInteger(context, "port" + (i+1), 53) :
                        Preferences.getInteger(context, "port" + (i-1) + "v6", 53);
            }
        }
        return 53;
    }
}
