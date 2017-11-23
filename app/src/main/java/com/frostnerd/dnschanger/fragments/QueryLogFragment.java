package com.frostnerd.dnschanger.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.adapters.QueryLogAdapter;
import com.frostnerd.dnschanger.database.accessors.QueryLogger;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.util.Util;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class QueryLogFragment extends Fragment implements SearchView.OnQueryTextListener{
    private QueryLogAdapter queryLogAdapter;
    private RecyclerView list;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_query_log, container, false);
        list = contentView.findViewById(R.id.list);
        layoutManager = new LinearLayoutManager(getContext());
        list.setLayoutManager(layoutManager);
        list.setAdapter(queryLogAdapter = new QueryLogAdapter(getContext(), contentView.findViewById(R.id.progress),
                (TextView)contentView.findViewById(R.id.row_count)));
        return contentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        QueryLogger.setNewQueryLoggedCallback(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        queryLogAdapter.reloadData();
        QueryLogger.setNewQueryLoggedCallback(new Runnable() {
            final Handler main = new Handler(Looper.getMainLooper());

            @Override
            public void run() {
                if(queryLogAdapter == null)return;
                queryLogAdapter.newQueryLogged(Util.getDBHelper(getContext()).getHighestRowID(DNSQuery.class));
                if(!list.isComputingLayout()){
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            queryLogAdapter.notifyItemInserted(0);
                            if(layoutManager.findFirstVisibleItemPosition() <= 2){
                                list.scrollToPosition(0);
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_rules, menu);

        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(Util.getActivity(this).getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context == null ? MainActivity.currentContext : context;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        queryLogAdapter.filter(QueryLogAdapter.ArgumentBasedFilter.HOST_SEARCH, query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(newText.equals("")){
            queryLogAdapter.removeFilters(QueryLogAdapter.ArgumentBasedFilter.HOST_SEARCH);
            if(!list.isComputingLayout())list.scrollToPosition(0);
            return true;
        }
        return false;
    }
}
