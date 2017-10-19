package com.frostnerd.dnschanger.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.preferences.AppCompatPreferenceActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class AdvancedSettingsActivity extends AppCompatPreferenceActivity {
    private boolean dialogShown = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        findPreference("advanced_settings").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("advanced_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final SwitchPreference pref = (SwitchPreference)preference;
                if(pref.isChecked()){
                    pref.setChecked(false);
                    showWarrantyDialog();
                }
                return true;
            }
        });
        findPreference("custom_port").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("rules_activated").setOnPreferenceChangeListener(preferenceChangeListener);
    }

    Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            setResult(RESULT_FIRST_USER);
            if (preference.getKey().equals("advanced_settings")) {
                if(!dialogShown && ((Boolean)o)) {
                    showWarrantyDialog();
                    return false;
                }
            }
            return true;
        }
    };

    private void showWarrantyDialog(){
        dialogShown = true;
        ((SwitchPreference)findPreference("advanced_settings")).setChecked(false);
        new AlertDialog.Builder(AdvancedSettingsActivity.this).setTitle(R.string.warning).setMessage(R.string.information_advanced_settings_warranty).setCancelable(true).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((SwitchPreference)findPreference("advanced_settings")).setChecked(true);
                        setResult(RESULT_FIRST_USER);
                        dialogShown = false;
                        dialog.dismiss();
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogShown = false;
            }
        }).show();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
