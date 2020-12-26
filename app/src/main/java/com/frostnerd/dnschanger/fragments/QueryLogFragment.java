package com.frostnerd.dnschanger.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.QueryLogAdapter;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.accessors.QueryLogger;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.general.Utils;

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
public class QueryLogFragment extends Fragment implements SearchView.OnQueryTextListener{
    private QueryLogAdapter queryLogAdapter;
    private RecyclerView list;
    private LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_query_log, container, false);
        list = contentView.findViewById(R.id.list);
        layoutManager = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layoutManager);
        list.setAdapter(queryLogAdapter = new QueryLogAdapter(requireContext(), contentView.findViewById(R.id.progress),
                (TextView)contentView.findViewById(R.id.row_count)));
        return contentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        QueryLogger.setNewQueryLoggedCallback(null);
    }

    @Override
    public void onDestroy() {
        queryLogAdapter.destroy();
        list.setAdapter(null);
        queryLogAdapter = null;
        list = null;
        layoutManager = null;
        super.onDestroy();
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
                queryLogAdapter.newQueryLogged(DatabaseHelper.getInstance(requireContext()).getHighestRowID(DNSQuery.class));
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

        SearchManager searchManager = Utils.requireNonNull((SearchManager) requireContext().getSystemService(Context.SEARCH_SERVICE));
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(Util.getActivity(this).getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
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
