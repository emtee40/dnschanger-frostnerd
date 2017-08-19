package com.frostnerd.dnschanger.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.preferences.searchablepreferences.SearchSettings;
import com.frostnerd.utils.preferences.searchablepreferences.v14.PreferenceSearcher;
import com.frostnerd.utils.preferences.searchablepreferences.v14.SearchablePreference;

import java.util.regex.Pattern;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class SettingsActivity extends AppCompatActivity implements SearchablePreference, SearchView.OnQueryTextListener {
    private PreferenceFragment preferenceFragment;
    private PreferenceSearcher preferenceSearcher = new PreferenceSearcher(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_settings);
        preferenceFragment = ((PreferenceFragment)getFragmentManager().findFragmentById(R.id.settings_fragment));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Pattern emptySearchPattern = Pattern.compile("[\\s]*?");
    @Override
    public boolean preferenceMatches(Preference preference, String search) {
        if(search == null || search.equals("") || emptySearchPattern.matcher(search).matches())return true;
        Pattern pattern = Pattern.compile("(?i).*?" + search + ".*");
        if(preference.getTitle() == null && preference.getSummary() != null){
            return pattern.matcher(preference.getSummary()).matches();
        }else if (preference.getSummary() == null && preference.getTitle() != null) {
            return pattern.matcher(preference.getTitle()).matches();
        } else
            return preference.getSummary() != null && pattern.matcher(preference.getTitle() + "" + preference.getSummary()).matches();
    }

    @Override
    public SearchSettings getSearchOptions() {
        return new SearchSettings.Builder().hideCategoriesWithNoChildren(true).matchCategories(false).build();
    }

    @Override
    public android.support.v7.preference.PreferenceGroup getTopLevelPreferenceGroup() {
        return preferenceFragment.getPreferenceScreen();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        preferenceSearcher.search(newText);
        return true;
    }
}
