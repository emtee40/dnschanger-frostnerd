package com.frostnerd.dnschanger;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.view.MenuItem;

import com.frostnerd.utils.preferences.AppCompatPreferenceActivity;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findPreference("setting_start_boot").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_show_notification").setOnPreferenceChangeListener(changeListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Preferences.put(SettingsActivity.this,preference.getKey(),newValue);
            if(preference.getKey().equalsIgnoreCase("setting_show_notification") && API.checkVPNServiceRunning(SettingsActivity.this))startService(new Intent(SettingsActivity.this, DNSVpnService.class));
            return true;
        }
    };
}
