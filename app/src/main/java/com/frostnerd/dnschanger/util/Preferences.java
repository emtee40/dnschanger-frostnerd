package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.frostnerd.utils.preferences.restrictions.PreferenceRestriction;
import com.frostnerd.utils.preferences.restrictions.PreferencesRestrictionBuilder;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
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
        builder.key("dns1").shouldNotBe(null).always().shouldNotBe("").always().shouldBeLike(Util.ipv4WithPort).always().doneWithKey();
        builder.key("dns1-v6").shouldNotBe(null).always().shouldNotBe("").always().shouldBeLike(Util.ipv6WithPort).always().doneWithKey();
        builder.key("setting_ipv6_enabled").shouldNotBe(false).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.utils.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv4_enabled", true);
            }
        }).doneWithKey();
        builder.key("setting_ipv4_enabled").shouldNotBe(false).when(new PreferenceRestriction.Condition() {
            @Override
            public boolean isMet(@NonNull com.frostnerd.utils.preferences.Preferences preferences, @NonNull String key, @Nullable Object newValue) {
                return !preferences.getBoolean("setting_ipv6_enabled", true);
            }
        }).doneWithKey();
        restrict(builder.build());
    }
}
