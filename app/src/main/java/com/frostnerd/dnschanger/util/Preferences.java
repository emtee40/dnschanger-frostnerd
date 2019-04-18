package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.utils.preferences.restrictions.PreferenceRestriction;
import com.frostnerd.utils.preferences.restrictions.PreferencesRestrictionBuilder;
import com.frostnerd.utils.preferences.restrictions.Type;

import java.util.Arrays;

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
public class Preferences extends com.frostnerd.utils.preferences.Preferences {
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
            public boolean isMet(@NonNull com.frostnerd.utils.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv4_enabled", true);
            }
        }).doneWithKey();
        builder.key("setting_ipv4_enabled").ofType(Type.BOOLEAN).shouldNotBe(false).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.utils.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv6_enabled", true);
            }
        }).doneWithKey();
        builder.key("setting_show_notification").ofType(Type.BOOLEAN).shouldBe(true).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.utils.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
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
