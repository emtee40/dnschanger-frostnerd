package com.frostnerd.dnschanger.activities;

import android.os.Bundle;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        // TODO Show dialog that no warranty is offered
    }
}
