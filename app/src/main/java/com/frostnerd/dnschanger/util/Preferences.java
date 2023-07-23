package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.preferenceexport.PreferenceHelper;
import com.frostnerd.preferences.restrictions.PreferenceRestriction;
import com.frostnerd.preferences.restrictions.PreferencesRestrictionBuilder;
import com.frostnerd.preferences.restrictions.Type;
import com.frostnerd.preferences.util.CustomBackendSharedPreference;
import com.frostnerd.preferences.util.ObscuredSharedPreferences;
import com.frostnerd.preferences.util.backends.SQLiteOpenHelperBackend;
import com.frostnerd.preferences.util.backends.SharedPreferencesBackend;
import com.frostnerd.preferences.util.obscureres.Base64Obscurer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
public class Preferences extends com.frostnerd.preferences.Preferences {
    private static Preferences instance;

    public static Preferences getInstance(Context context){
        if(instance == null){
            return instance = new Preferences(createPreferences(context));
        } else return instance;
    }

    private static SharedPreferences createPreferences(@NonNull Context context){
        SharedPreferences sharedPreferences;

        ObscuredSharedPreferences obscuredSharedPreferences;
        SQLiteOpenHelperBackend backend = new SQLiteOpenHelperBackend(DatabaseHelper.getInstance(context));
        CustomBackendSharedPreference customBackendSharedPreference = new CustomBackendSharedPreference(backend);
        obscuredSharedPreferences = new ObscuredSharedPreferences(customBackendSharedPreference, new Base64Obscurer(true, true));

        if(obscuredSharedPreferences.getAll().size() != 0) {
            sharedPreferences = CustomBackendSharedPreference.convertFromOldBackend(new SharedPreferencesBackend(obscuredSharedPreferences), new SharedPreferencesBackend(getDefaultPreferences(context)));
            obscuredSharedPreferences.edit().clear().commit();
        } else {
            sharedPreferences = getDefaultPreferences(context);
        }
        return sharedPreferences;
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
                "first_run", "rated", "auto_pause", "app_whitelist_configured", "ipv6_asked",
                "start_service_when_available"};
        for(String s: booleanPreferences){
            builder.key(s).ofType(Type.BOOLEAN).doneWithKey();
        }

        builder.key("theme").ofType(Type.STRING).shouldBeOneOf(Arrays.asList("1", "2", "3", "4")).always().doneWithKey();
        builder.key("tcp_timeout").ofType(Type.STRING).ofSecondaryType(Type.INTEGER).doneWithKey();
        builder.key("launches").ofType(Type.INTEGER).doneWithKey();
        builder.key("autopause_apps_count").ofType(Type.INTEGER).doneWithKey();
        builder.key("autopause_apps").ofType(Type.ANY_SAVEABLE).doneWithKey();
        builder.key("dialogtheme").ofType(Type.INTEGER).shouldBeOneOf(Arrays.asList(1, 2, 3, 4)).always()
                .nextKey("apptheme").ofType(Type.INTEGER).shouldBeOneOf(Arrays.asList(1, 2, 3, 4)).always().doneWithKey();
        restrict(builder.build());
    }

    public static String exportToJson(Context context) throws JSONException {
        Set<String> excludedKeys = new HashSet<>();
        excludedKeys.add("first_run");
        excludedKeys.add("device_admin");
        excludedKeys.add("launches");
        excludedKeys.add("rated");

        Map<String, JsonElement> additionalData = new HashMap<>();
        Map<String, Object> headers = new HashMap<>();
        List<DNSEntry> customEntries = DatabaseHelper.getInstance(context).getCustomDNSEntries();
        if(customEntries.size() != 0)
            additionalData.put("dnsentries", exportDNSEntries(customEntries));

        return PreferenceHelper.exportToJSON(Preferences.getInstance(context), false, excludedKeys, headers, additionalData).toString();
    }

    public static void importFromJson(Context context, String jsonString) throws JSONException {
        PreferenceHelper.ImportPayload payload = PreferenceHelper.importFromJSON(getInstance(context),
                new Gson().fromJson(jsonString, JsonObject.class));
        if(payload.getPayload().containsKey("dnsentries")){
            importDNSEntries((JsonArray) payload.getPayload().get("dnsentries"), context);
        }
    }

    private static JsonArray exportDNSEntries(Collection<DNSEntry> entries) throws JSONException {
        JsonArray entryBase = new JsonArray();
        for(DNSEntry entry: entries){
            JsonObject current = new JsonObject();
            current.addProperty("name", entry.getName());
            current.addProperty("shortname", entry.getShortName());
            current.addProperty("description", entry.getDescription());
            current.addProperty("dns1", entry.getDns1().toString(true));
            if(entry.getDns2() != null)current.addProperty("dns2", entry.getDns2().toString(true));
            current.addProperty("dns1v6", entry.getDns1V6().toString(true));
            if(entry.getDns2V6() != null)current.addProperty("dns2v6", entry.getDns2V6().toString(true));
            entryBase.add(current);
        }
        return entryBase;
    }

    private static void importDNSEntries(JsonArray jsonElements, Context context){
        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        for (int i = 0; i < jsonElements.size(); i++) {
            JsonObject current = jsonElements.get(i).getAsJsonObject();
            databaseHelper.insert(new DNSEntry(
                    findUnusedDNSEntryName(databaseHelper, current.get("name").getAsString()),
                    current.get("shortname").getAsString(),
                    IPPortPair.wrap(current.get("dns1").getAsString()),
                    current.has("dns2") ? IPPortPair.wrap(current.get("dns2").getAsString()) : null,
                    IPPortPair.wrap(current.get("dns1v6").getAsString()),
                    current.has("dns2v6") ? IPPortPair.wrap(current.get("dns2v6").getAsString()) : null,
                    current.get("description").getAsString(),
                    true
            ));
        }
    }

    private static String findUnusedDNSEntryName(DatabaseHelper helper, String name){
        for (int i = 0; i < 100; i++) {
            String newName = i == 0 ? name : i + "_" + name;
            if(!helper.dnsEntryExists(newName)) return newName;
        }
        return name;
    }
}
