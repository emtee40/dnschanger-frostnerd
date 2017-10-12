package com.frostnerd.dnschanger.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.dialogs.NewRuleDialog;
import com.frostnerd.dnschanger.util.API;
import com.frostnerd.dnschanger.util.DatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RuleAdapter extends RecyclerView.Adapter<RuleAdapter.ViewHolder>{
    private DatabaseHelper databaseHelper;
    private LayoutInflater inflater;
    private int count;
    private String search = "";
    private List<Integer> rows = new ArrayList<>();
    private HashMap<Integer,Integer> rowRemap = new HashMap<>();
    private boolean update = true;
    private Activity context;
    private HashMap<Filter, String> filterValues = new HashMap<>();
    private TextView rowCount;

    public RuleAdapter(Activity context, DatabaseHelper databaseHelper, TextView rowCount){
        this.databaseHelper = databaseHelper;
        this.context = context;
        inflater = LayoutInflater.from(context);
        filterValues.put(ArgumentBasedFilter.SHOW_WILDCARD_ONLY, "0");
        this.rowCount = rowCount;
        reloadData();
    }

    public void setUpdateDataOnConfigChange(boolean update) {
        this.update = update;
    }

    public void removeFilters(ArgumentBasedFilter... filters){
        for(ArgumentBasedFilter filter: filters){
            filterValues.remove(filter);
        }
        reloadData();
    }

    public void filter(ArgumentBasedFilter filter, String argument){
        if(argument.equals(""))filterValues.remove(filter);
        else{
            if(filterValues.containsKey(ArgumentBasedFilter.SHOW_IPV6_ONLY) && filter == ArgumentBasedFilter.SHOW_IPV4_ONLY){
                filterValues.remove(ArgumentBasedFilter.SHOW_IPV6_ONLY);
            }else if(filterValues.containsKey(ArgumentBasedFilter.SHOW_IPV4_ONLY) && filter == ArgumentBasedFilter.SHOW_IPV6_ONLY){
                filterValues.remove(ArgumentBasedFilter.SHOW_IPV4_ONLY);
            }
            filterValues.put(filter, argument);
        }
        reloadData();
    }

    public boolean hasFilter(ArgumentBasedFilter filter){
        return filterValues.containsKey(filter);
    }

    public String getFilterValue(ArgumentBasedFilter filter){
        return filterValues.get(filter);
    }

    public void setWildcardMode(boolean wildcard, boolean resetSearch){
        if(resetSearch)search = "";
        filterValues.remove(ArgumentBasedFilter.HOST_SEARCH);
        filter(ArgumentBasedFilter.SHOW_WILDCARD_ONLY, wildcard ? "1" : "0");
    }

    public void reloadData(){
        if(!update)return;
        evaluateData();
        notifyDataSetChanged();
    }

    private void evaluateData(){
        Cursor cursor;
        rows.clear();
        rowRemap.clear();
        if(!search.equals("")){
            if(!search.equals(""))cursor = databaseHelper.getReadableDatabase().rawQuery(constructQuery("SELECT ROWID FROM DNSRules"), null);
            else cursor = databaseHelper.getReadableDatabase().rawQuery(constructQuery("SELECT ROWID FROM DNSRules"), null);
            count = cursor.getCount();
            if(cursor.moveToFirst()){
                do{
                    rows.add((int)cursor.getLong(0));
                }while(cursor.moveToNext());
            }
            cursor.close();
        }else{
            cursor = databaseHelper.getReadableDatabase().rawQuery(constructQuery("SELECT ROWID FROM DNSRules"), null);
            if(cursor.moveToFirst()){
                this.count = cursor.getCount();
                int count = 1, rawCount = 0, id;
                do{
                    id = (int)cursor.getLong(0);
                    if(count++ != id){
                        rowRemap.put(rawCount, id);
                        count = id;
                    }
                    rawCount++;
                }while(cursor.moveToNext());
            }else count = 0;
            cursor.close();
        }
        rowCount.setText(context.getString(R.string.x_entries).replace("[x]", count + ""));
    }

    private String constructQuery(String base){
        if(filterValues.size() == 0)return base;
        String query = base + " WHERE ", newQuery;
        for(Filter filter: filterValues.keySet()){
            newQuery = filter.appendToQuery(query, filterValues.get(filter));
            if(!newQuery.equals(query))query = newQuery + " AND ";
        }
        return query.substring(0, query.length() - 4);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.row_rule, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Cursor cursor;
        if(!search.equals(""))cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target, Wildcard FROM DNSRules WHERE ROWID=" + rows.get(position), null);
        else {
            int rowID = rowRemap.containsKey(position) ? rowRemap.get(position) : position+1;
            cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target, Wildcard FROM DNSRules WHERE ROWID=" + rowID, null);
        }
        cursor.moveToFirst();
        final String host = cursor.getString(cursor.getColumnIndex("Domain")),
                target = cursor.getString(cursor.getColumnIndex("Target"));
        final boolean ipv6 = cursor.getInt(cursor.getColumnIndex("IPv6")) == 1,
                wildcard = cursor.getInt(cursor.getColumnIndex("Wildcard")) == 1;
        ((TextView)holder.itemView.findViewById(R.id.text)).setText(host);
        ((TextView)holder.itemView.findViewById(R.id.text3)).setText(target);
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new NewRuleDialog(context, new NewRuleDialog.CreationListener() {
                    @Override
                    public void creationFinished(@NonNull String host, @Nullable String target, @Nullable String targetV6, boolean ipv6, boolean wildcard, boolean editingMode) {
                        if(target != null)API.getDBHelper(context).editDNSRule(host, ipv6, target);
                        else{
                            API.getDBHelper(context).deleteDNSRule(host, ipv6);
                        }
                        reloadData();
                    }
                }, host, target, wildcard, ipv6).show();
                return true;
            }
        });
        cursor.close();
    }

    @Override
    public int getItemCount() {
        return count;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    private interface Filter{
        public String appendToQuery(String query, String argument);
    }

    public enum ArgumentBasedFilter implements Filter{
        TARGET {
            @Override
            public String appendToQuery(String query, String argument) {
                return query + "Target LIKE '%" + argument + "%'";
            }
        }, SHOW_IPV6_ONLY{
            @Override
            public String appendToQuery(String query, String argument) {
                if(argument.equals("0"))return query;
                return query + "IPv6=1";
            }
        }, HIDE_LOCAL {
            @Override
            public String appendToQuery(String query, String argument) {
                if(argument.equals("1"))return query + "Target!='127.0.0.1' AND Target!='::1'";
                else return query;
            }
        }, SHOW_WILDCARD_ONLY {
            @Override
            public String appendToQuery(String query, String argument) {
                return query + "Wildcard=" + argument;
            }
        }, HOST_SEARCH{
            @Override
            public String appendToQuery(String query, String argument) {
                return query + "Domain LIKE '%" + argument + "%'";
            }
        }, SHOW_IPV4_ONLY{
            @Override
            public String appendToQuery(String query, String argument) {
                if(argument.equals("0"))return query;
                return query + "IPV6=0";
            }
        }
    }
}
