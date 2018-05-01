package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.frostnerd.preferences.restrictions.PreferenceRestriction;
import com.frostnerd.preferences.restrictions.PreferencesRestrictionBuilder;
import com.frostnerd.preferences.restrictions.Type;

import java.util.Arrays;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class Preferences extends com.frostnerd.preferences.Preferences {
    private static Preferences instance;

    public static Preferences getInstance(Context context){
        if(instance == null)return instance = new Preferences(getDefaultPreferences(context));
        else return instance;
    }

    public Preferences(SharedPreferences sharedPreferences) {
        super(sharedPreferences);
        setRestrictions();
    }

    private void setRestrictions(){
        PreferencesRestrictionBuilder builder = new PreferencesRestrictionBuilder();
        builder.key("dns1").ofType(Type.STRING).shouldNotBe(null).always().shouldNotBe("").always().shouldBeLike(Util.ipv4WithPort).always().doneWithKey();
        builder.key("dns1-v6").ofType(Type.STRING).shouldNotBe(null).always().shouldNotBe("").always().shouldBeLike(Util.ipv6WithPort).always().doneWithKey();
        builder.key("dns2").ofType(Type.STRING).shouldNotBe(null).always().shouldBeLike(Util.ipv4WithPort)
                .whenToStringIsNotEmpty().doneWithKey();
        builder.key("dns2-v6").ofType(Type.STRING).shouldNotBe(null).always().shouldBeLike(Util.ipv6WithPort)
                .whenToStringIsNotEmpty().doneWithKey();

        builder.key("setting_ipv6_enabled").ofType(Type.BOOLEAN).shouldNotBe(false).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv4_enabled", true);
            }
        }).doneWithKey();
        builder.key("setting_ipv4_enabled").ofType(Type.BOOLEAN).shouldNotBe(false).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv6_enabled", true);
            }
        }).doneWithKey();
        builder.key("setting_show_notification").ofType(Type.BOOLEAN).shouldBe(true).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
            }
        }).doneWithKey();
        builder.key("pin_value").ofType(Type.STRING).shouldNotBe("").always().shouldNotBe(null).always().doneWithKey();

        String[] booleanPreferences = {"hide_notification_icon", "notification_on_stop", "setting_start_boot",
                "setting_auto_wifi", "setting_auto_mobile", "setting_disable_netchange", "setting_pin_enabled",
                "pin_fingerprint", "pin_notification", "pin_tile", "pin_app_shortcut", "shortcut_click_again_disable",
                "excluded_whitelist", "device_admin", "setting_app_shortcuts_enabled", "check_connectivity", "debug",
                "advanced_settings", "loopback_allowed", "custom_port", "rules_activated", "dns_over_tcp", "query_logging",
                "first_run", "rated", "auto_pause", "everything_disabled", "app_whitelist_configured", "ipv6_asked",
                "start_service_when_available"};
        for(String s: booleanPreferences){
            builder.key(s).ofType(Type.BOOLEAN).doneWithKey();
        }

        builder.key("theme").ofType(Type.STRING).shouldBeOneOf(Arrays.asList("1", "2", "3")).always().doneWithKey();
        builder.key("tcp_timeout").ofType(Type.INTEGER).doneWithKey();
        builder.key("launches").ofType(Type.INTEGER).doneWithKey();
        builder.key("autopause_apps_count").ofType(Type.INTEGER).doneWithKey();
        builder.key("autopause_apps").ofType(Type.ANY_SAVEABLE).doneWithKey();
        builder.key("dialogtheme").ofType(Type.INTEGER).shouldBeOneOf(Arrays.asList(1,2,3)).always()
                .nextKey("apptheme").ofType(Type.INTEGER).shouldBeOneOf(Arrays.asList(1,2,3)).always().doneWithKey();
        restrict(builder.build());
    }
}
