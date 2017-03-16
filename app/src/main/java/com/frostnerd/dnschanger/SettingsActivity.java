package com.frostnerd.dnschanger;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v7.app.AlertDialog;
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
        findPreference("show_used_dns").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_auto_mobile").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_pin_enabled").setOnPreferenceChangeListener(changeListener);
        findPreference("pin_value").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getString(R.string.summary_pin_value).replace("[[x]]", ""+newValue));
                return true;
            }
        });
        findPreference("pin_value").setSummary(getString(R.string.summary_pin_value).replace("[[x]]", Preferences.getString(this, "pin_value", "1234")));
        if(API.isTaskerInstalled(this))findPreference("warn_automation_tasker").setSummary(R.string.summary_automation_warn);
        else ((PreferenceCategory)findPreference("automation")).removePreference(findPreference("warn_automation_tasker"));
        findPreference("setting_info").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.information).setMessage(R.string.settings_information_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
                return true;
            }
        });
        findPreference("contact_dev").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        findPreference("autopause_appselect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return false;
            }
        });
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
            String key = preference.getKey();
            if((key.equalsIgnoreCase("setting_show_notification") || key.equalsIgnoreCase("show_used_dns")) && API.checkVPNServiceRunning(SettingsActivity.this))startService(new Intent(SettingsActivity.this, DNSVpnService.class));
            return true;
        }
    };
}
