package com.frostnerd.dnschanger.util;

import android.content.Context;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;

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

    public static boolean isEverythingDisabled(Context context){
        return Preferences.getBoolean(context, "everything_disabled", false);
    }

    public static boolean checkConnectivityOnStart(Context context){
        return Preferences.getBoolean(context, "check_connectivity", false);
    }

    public static boolean isDebugEnabled(Context context){
        return Preferences.getBoolean(context, "debug", false);
    }

    public static boolean shouldHideNotificationIcon(Context context){
        return Preferences.getBoolean(context, "hide_notification_icon", false);
    }

    public static boolean isNotificationEnabled(Context context){
        return Preferences.getBoolean(context, "setting_show_notification", true);
    }

    public static boolean areAppShortcutsEnabled(Context context){
        return Preferences.getBoolean(context, "setting_app_shortcuts_enabled", false);
    }

    public static boolean isAdvancedModeEnabled(Context context){
        return Preferences.getBoolean(context, "advanced_settings", false);
    }

    public static boolean isRunningInAdvancedMode(Context context){
        return areCustomPortsEnabled(context) ||
                areRulesEnabled(context) ||
                isQueryLoggingEnabled(context) ||
                sendDNSOverTCP(context);
    }

    public static boolean areCustomPortsEnabled(Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getBoolean(context, "custom_port", false);
    }

    public static boolean areRulesEnabled(Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getBoolean(context, "rules_activated", false);
    }

    public static boolean isQueryLoggingEnabled(Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getBoolean(context, "query_logging", false);
    }

    public static boolean isPinProtectionEnabled(Context context){
        return Preferences.getBoolean(context, "setting_pin_enabled", false);
    }

    public static String getPinCode(Context context){
        return Preferences.getString(context, "pin_value", "1234");
    }

    public static boolean canUseFingerprintForPin(Context context){
        return Preferences.getBoolean(context, "pin_fingerprint", false);
    }

    public static boolean sendDNSOverTCP(Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getBoolean(context, "dns_over_tcp", false);
    }

    public static int getTCPTimeout(Context context){
        return Integer.parseInt(Preferences.getString(context,"tcp_timeout", "500"));
    }

    private static String getDNS1(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns1", "8.8.8.8") : "";
    }

    private static String getDNS2(Context context) {
        return isIPv4Enabled(context) ? Preferences.getString(context, "dns2", "8.8.4.4") : "";
    }

    private static String getDNS1V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns1-v6", "2001:4860:4860::8888") : "";
    }

    private static String getDNS2V6(Context context) {
        return isIPv6Enabled(context) ? Preferences.getString(context, "dns2-v6", "2001:4860:4860::8844") : "";
    }

    public static boolean isPinProtected(Context context, PinProtectable pinProtectable){
        return pinProtectable.isEnabled(context);
    }

    public static ArrayList<IPPortPair> getAllDNSPairs(final Context context, final boolean enabledOnly){
        return new ArrayList<IPPortPair>(){
            private final boolean customPorts = areCustomPortsEnabled(context);
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

    public static boolean shouldShowVPNInfoDialog(Context context){
        return Preferences.getBoolean(context, "show_vpn_info", true);
    }

    public static void setShowVPNInfoDialog(Context context, boolean doShow){
        Preferences.put(context, "show_vpn_info", doShow);
    }

    public enum Type{
        DNS1("dns1", "port1", "8.8.8.8"),
        DNS2("dns2", "port2", "8.8.4.4"),
        DNS1_V6("dns1-v6", "port1v6", "2001:4860:4860::8888"),
        DNS2_V6("dns2-v6", "port2v6", "2001:4860:4860::8844");

        private final String dnsKey, portKey, defaultAddress;
        Type(String dnsKey, String portKey, String defaultAddress){
            this.dnsKey = dnsKey;
            this.portKey = portKey;
            this.defaultAddress = defaultAddress;
        }

        public static Type fromKey(String key){
            for(Type val: values()){
                if(val.dnsKey.equalsIgnoreCase(key))return val;
            }
            return null;
        }

        private boolean isIPv6(){
            return dnsKey.contains("v6");
        }

        private boolean isAddressTypeEnabled(Context context){
            return (isIPv6() && isIPv6Enabled(context)) || (!isIPv6() && isIPv4Enabled(context));
        }

        private int getPort(Context context){
            return areCustomPortsEnabled(context) ? Preferences.getInteger(context, portKey, 53) : 53;
        }

        private String getServerAddress(Context context){
            return Preferences.getString(context, dnsKey, defaultAddress);
        }

        public IPPortPair getPair(Context context){
            return new IPPortPair(getServerAddress(context), getPort(context), isIPv6());
        }

        public DNSEntry findMatchingDatabaseEntry(Context context){
            String address = "%" + getServerAddress(context) + "%";
            ParsedEntity<DNSEntry> parsedEntity = Util.getDBHelper(context).getSQLHandler(DNSEntry.class);
            return parsedEntity.selectFirstRow(Util.getDBHelper(context)
            , false, WhereCondition.like(parsedEntity.getTable().findColumn("dns1"), address).nextOr(),
                    WhereCondition.like(parsedEntity.getTable().findColumn("dns2"), address).nextOr(),
                    WhereCondition.like(parsedEntity.getTable().findColumn("dns1v6"), address).nextOr(),
                    WhereCondition.like(parsedEntity.getTable().findColumn("dns2v6"), address));
        }

        public void saveDNSPair(Context context, IPPortPair pair){
            if(!canBeEmpty() && pair.isEmpty())return;
            Preferences.put(context, portKey, pair.getPort() == -1 ? 53 : pair.getPort());
            Preferences.put(context, dnsKey, pair.getAddress());
        }

        public boolean canBeEmpty(){
            return this == DNS2 || this == DNS2_V6;
        }
    }

    public enum PinProtectable{
        APP("pin_app"), APP_SHORTCUT("pin_app_shortcut"), TILE("pin_tile"), NOTIFICATION("pin_notification");

        private final String settingsKey;
        PinProtectable(String settingsKey){
            this.settingsKey = settingsKey;
        }

        private boolean isEnabled(Context context){
            return Preferences.getBoolean(context, "setting_pin_enabled", false) &&
                    Preferences.getBoolean(context, settingsKey, false);
        }

    }
}
