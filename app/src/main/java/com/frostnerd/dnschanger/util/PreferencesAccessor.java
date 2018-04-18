package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;

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

    public static boolean isIPv6Enabled(@NonNull Context context) {
        return Preferences.getInstance(context).getBoolean( "setting_ipv6_enabled", false);
    }

    public static boolean isIPv4Enabled(@NonNull Context context) {
        return Preferences.getInstance(context).getBoolean(  "setting_ipv4_enabled", true);
    }

    public static void setIPv4Enabled(@NonNull Context context, boolean enabled ){
        Preferences.getInstance(context).put(  "setting_ipv4_enabled", enabled);
    }

    public static void setIPv6Enabled(@NonNull Context context, boolean enabled ){
        Preferences.getInstance(context).put(  "setting_ipv6_enabled", enabled);
    }

    public static boolean isEverythingDisabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "everything_disabled", false);
    }

    public static boolean checkConnectivityOnStart(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "check_connectivity", false);
    }

    public static boolean isDebugEnabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "debug", false);
    }

    public static boolean shouldHideNotificationIcon(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "hide_notification_icon", false);
    }

    public static boolean isNotificationEnabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "setting_show_notification", true);
    }

    public static boolean areAppShortcutsEnabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "setting_app_shortcuts_enabled", false);
    }

    public static boolean isAdvancedModeEnabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "advanced_settings", false);
    }

    public static boolean isLoopbackAllowed(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "loopback_allowed", false);
    }

    public static boolean isRunningInAdvancedMode(@NonNull Context context){
        return areCustomPortsEnabled(context) ||
                areRulesEnabled(context) ||
                isQueryLoggingEnabled(context) ||
                sendDNSOverTCP(context) || isLoopbackAllowed(context) ||
                sendDNSOverTLS(context);
    }

    public static boolean areCustomPortsEnabled(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "custom_port", false);
    }

    public static boolean areRulesEnabled(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "rules_activated", false);
    }

    public static boolean isQueryLoggingEnabled(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "query_logging", false);
    }

    public static boolean isPinProtectionEnabled(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "setting_pin_enabled", false);
    }

    @NonNull
    public static String getPinCode(@NonNull Context context){
        return Preferences.getInstance(context).getString(  "pin_value", "1234");
    }

    public static boolean canUseFingerprintForPin(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "pin_fingerprint", false);
    }

    public static boolean sendDNSOverTCP(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "dns_over_tcp", false);
    }

    public static boolean sendDNSOverTLS(@NonNull Context context){
        return isAdvancedModeEnabled(context) &&
                Preferences.getInstance(context).getBoolean(  "dns_over_tls", false);
    }

    public static int getTCPTimeout(@NonNull Context context){
        return Integer.parseInt(Preferences.getInstance(context).getString( "tcp_timeout", "500"));
    }

    @NonNull
    private static String getDNS1(@NonNull Context context) {
        return isIPv4Enabled(context) ? Preferences.getInstance(context).getString(  "dns1", "8.8.8.8") : "";
    }

    @NonNull
    private static String getDNS2(@NonNull Context context) {
        return isIPv4Enabled(context) ? Preferences.getInstance(context).getString(  "dns2", "8.8.4.4") : "";
    }

    @NonNull
    private static String getDNS1V6(@NonNull Context context) {
        return isIPv6Enabled(context) ? Preferences.getInstance(context).getString(  "dns1-v6", "2001:4860:4860::8888") : "";
    }

    @NonNull
    private static String getDNS2V6(@NonNull Context context) {
        return isIPv6Enabled(context) ? Preferences.getInstance(context).getString(  "dns2-v6", "2001:4860:4860::8844") : "";
    }

    @NonNull
    public static boolean isPinProtected(@NonNull Context context, PinProtectable pinProtectable){
        return pinProtectable.isEnabled(context);
    }

    @NonNull
    public static ArrayList<IPPortPair> getAllDNSPairs(@NonNull final Context context, final boolean enabledOnly){
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
                            Preferences.getInstance(context).getInteger(  "port" + (id >= 3 ? "2" : "1") + (id % 2 == 0 ? "v6" : ""), 53)
                            : 53;
                    add(new IPPortPair(s, port, id % 2 == 0));
                }
            }
        };
    }

    public static boolean shouldShowVPNInfoDialog(@NonNull Context context){
        return Preferences.getInstance(context).getBoolean(  "show_vpn_info", true);
    }

    public static void setShowVPNInfoDialog(@NonNull Context context, boolean doShow){
        Preferences.getInstance(context).put(  "show_vpn_info", doShow);
    }

    public enum Type{
        DNS1("dns1", "port1", "8.8.8.8"),
        DNS2("dns2", "port2", "8.8.4.4"),
        DNS1_V6("dns1-v6", "port1v6", "2001:4860:4860::8888"),
        DNS2_V6("dns2-v6", "port2v6", "2001:4860:4860::8844");

        private final String dnsKey, portKey, defaultAddress;
        Type(@NonNull String dnsKey, @NonNull String portKey, @NonNull String defaultAddress){
            this.dnsKey = dnsKey;
            this.portKey = portKey;
            this.defaultAddress = defaultAddress;
        }

        @Nullable
        public static Type fromKey(@NonNull String key){
            for(Type val: values()){
                if(val.dnsKey.equalsIgnoreCase(key))return val;
            }
            return null;
        }

        private boolean isIPv6(){
            return dnsKey.contains("v6");
        }

        private boolean isAddressTypeEnabled(@NonNull Context context){
            return (isIPv6() && isIPv6Enabled(context)) || (!isIPv6() && isIPv4Enabled(context));
        }

        private int getPort(@NonNull Context context){
            return areCustomPortsEnabled(context) ? Preferences.getInstance(context).getInteger(  portKey, 53) : 53;
        }

        @NonNull
        private String getServerAddress(@NonNull Context context){
            return Preferences.getInstance(context).getString(  dnsKey, defaultAddress);
        }

        @NonNull
        public IPPortPair getPair(@NonNull Context context){
            return new IPPortPair(getServerAddress(context), getPort(context), isIPv6());
        }

        @Nullable
        public DNSEntry findMatchingDatabaseEntry(@NonNull Context context){
            return DatabaseHelper.getInstance(context).findMatchingDNSEntry(getServerAddress(context));
        }

        public void saveDNSPair(@NonNull Context context, @NonNull IPPortPair pair){
            if(!canBeEmpty() && pair.isEmpty())return;
            Preferences.getInstance(context).put(  portKey, pair.getPort() == -1 ? 53 : pair.getPort());
            Preferences.getInstance(context).put(  dnsKey, pair.getAddress());
        }

        public boolean canBeEmpty(){
            return this == DNS2 || this == DNS2_V6;
        }
    }

    public enum PinProtectable{
        APP("pin_app"), APP_SHORTCUT("pin_app_shortcut"), TILE("pin_tile"), NOTIFICATION("pin_notification");

        @NonNull private final String settingsKey;
        PinProtectable(@NonNull String settingsKey){
            this.settingsKey = settingsKey;
        }

        private boolean isEnabled(Context context){
            return Preferences.getInstance(context).getBoolean(  "setting_pin_enabled", false) &&
                    Preferences.getInstance(context).getBoolean(  settingsKey, false);
        }

    }
}
