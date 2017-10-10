package com.frostnerd.dnschanger.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.adapters.RuleAdapter;
import com.frostnerd.dnschanger.util.API;
import com.frostnerd.utils.preferences.Preferences;

import java.io.IOException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RulesFragment extends Fragment implements SearchView.OnQueryTextListener {
    private View content;
    private RecyclerView list;
    private RuleAdapter ruleAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return content = inflater.inflate(R.layout.fragment_rules, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(!Preferences.getBoolean(getContext(), "db_debug", false)){
            try {
                API.getDBHelper(getContext()).loadEntries(getContext());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Preferences.put(getContext(), "db_debug", true);
        }
        list = content.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(ruleAdapter = new RuleAdapter(getContext(), API.getDBHelper(getContext())));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_rules, menu);

        SearchManager searchManager = (SearchManager)getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(API.getActivity(this).getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ruleAdapter.search(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context == null ? MainActivity.currentContext : context;
    }
}
