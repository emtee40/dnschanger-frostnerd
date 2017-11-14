package com.frostnerd.dnschanger.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.dialogs.NewRuleDialog;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.database.orm.parser.Column;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.LimitOption;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.QueryOption;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.utils.general.ArrayUtil;

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
    private static final int ROW_REMAP_FETCH_COUNT = 80, MAX_ROW_ID_CACHE_COUNT = 10000,
            ROW_REMAP_FETCH_COUNT_WHEN_SEARCHING = ROW_REMAP_FETCH_COUNT*3;
    private DatabaseHelper databaseHelper;
    private LayoutInflater inflater;
    private int count;
    private List<Integer> rows = new ArrayList<>();
    private HashMap<Integer,Integer> rowRemap = new HashMap<>();
    private boolean update = true;
    private Activity context;
    private HashMap<Filter, String> filterValues = new HashMap<>();
    private TextView rowCount;
    private ProgressBar updateProgress;
    private int rowRemapPos = 0;
    private static Column<DNSRule> ipv6Column, hostColumn, targetColumn, wildcardColumn, rowIDColumn;
    private WhereCondition[] whereConditions;

    public RuleAdapter(Activity context, DatabaseHelper databaseHelper, TextView rowCount, ProgressBar updateProgress){
        this.databaseHelper = databaseHelper;
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.rowCount = rowCount;
        this.updateProgress = updateProgress;
        filterValues.put(ArgumentLessFilter.SHOW_NORMAL, "0");
        filterValues.put(ArgumentLessFilter.SHOW_WILDCARD, "0");
        filterValues.put(ArgumentLessFilter.SHOW_IPV4, "0");
        filterValues.put(ArgumentLessFilter.SHOW_IPV6, "0");
        ipv6Column = databaseHelper.findColumn(DNSRule.class, "ipv6");
        hostColumn = databaseHelper.findColumn(DNSRule.class, "host");
        targetColumn = databaseHelper.findColumn(DNSRule.class, "target");
        wildcardColumn = databaseHelper.findColumn(DNSRule.class, "wildcard");
        rowIDColumn = databaseHelper.getRowIDColumn(DNSRule.class);
        reloadData();
    }

    public void setUpdateDataOnConfigChange(boolean update) {
        this.update = update;
    }

    public void removeFilters(Filter... filters){
        for(Filter filter: filters){
            filterValues.remove(filter);
        }
        reloadData();
    }

    public void removeFilters(ArgumentLessFilter... filters){
        for(Filter filter: filters){
            filterValues.remove(filter);
        }
        reloadData();
    }

    public void removeFilters(ArgumentBasedFilter... filters){
        for(Filter filter: filters){
            filterValues.remove(filter);
        }
        reloadData();
    }

    public void filter(ArgumentBasedFilter filter, String argument){
        filterValues.put(filter, argument);
        reloadData();
    }

    public void filter(ArgumentLessFilter filter){
        filterValues.put(filter, "");
        reloadData();
    }

    public boolean hasFilter(Filter filter){
        return filterValues.containsKey(filter);
    }

    public String getFilterValue(ArgumentBasedFilter filter){
        return filterValues.get(filter);
    }

    public void reloadData(){
        if(!update)return;
        List<WhereCondition> conditions = getFilterConditions();
        if(conditions != null)whereConditions = conditions.toArray(new WhereCondition[conditions.size()]);
        else whereConditions = new WhereCondition[0];
        updateProgress.setVisibility(View.VISIBLE);
        new Thread(){
            @Override
            public void run() {
                evaluateData();
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rowCount.setText(context.getString(R.string.x_entries).replace("[x]", count + ""));
                        notifyDataSetChanged();
                        updateProgress.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }.start();
    }

    private void evaluateData(){
        Cursor cursor;
        rows.clear();
        rowRemap.clear();
        rowRemapPos = 0;
        loadRowRemap(0);
        if(filterValues.containsKey(ArgumentBasedFilter.HOST_SEARCH)){
            cursor = databaseHelper.getSQLHandler(DNSRule.class).selectCursor(databaseHelper, false, new Column[]{rowIDColumn}, whereConditions);
            count = cursor.getCount();
            if(count > MAX_ROW_ID_CACHE_COUNT){
                loadRowRemap(0);
            }else{
                if(cursor.moveToFirst()){
                    do{
                        rows.add((int)cursor.getLong(0));
                    }while(cursor.moveToNext());
                }
            }
            cursor.close();
        }else{
            count = queryDBRuleCount();
            loadRowRemap(0);
        }
    }

    private int queryDBRuleCount(){
        return databaseHelper.getCount(DNSRule.class, whereConditions);
    }

    private void loadRowRemap(int position){
        if(rowRemap.size() == 0 || position+1 >= rowRemap.size()){
            int fetchCount = position > rowRemapPos ? position+1 :
                    (filterValues.containsKey(ArgumentBasedFilter.HOST_SEARCH) ?
                            ROW_REMAP_FETCH_COUNT_WHEN_SEARCHING : ROW_REMAP_FETCH_COUNT);
            Cursor cursor = databaseHelper.getSQLHandler(DNSRule.class).selectCursor(databaseHelper,
                    false, new Column[]{rowIDColumn}, ArrayUtil.combine(QueryOption.class, new LimitOption(fetchCount, rowRemapPos), whereConditions));
            if(cursor.moveToFirst()){
                int id, count = rowRemapPos+1, rawCount = rowRemapPos;
                do{
                    id = (int)cursor.getLong(0);
                    if(count++ != id){
                        rowRemap.put(rawCount, id);
                        count = id;
                    }
                    rawCount++;
                }while(cursor.moveToNext());
            }
            cursor.close();
            rowRemapPos += fetchCount;
        }
    }

    private List<WhereCondition> getFilterConditions(){
        if(filterValues.size() == 0)return null;
        List<WhereCondition> conditions = new ArrayList<>();
        WhereCondition condition;
        for(Filter filter: filterValues.keySet()){
            if((condition = filter.getCondition(filterValues.get(filter), filterValues)) != null){
                conditions.add(filter.getCondition(filterValues.get(filter), filterValues));
            }
        }
        return conditions;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.row_rule, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final DNSRule rule;
        if(filterValues.containsKey(ArgumentBasedFilter.HOST_SEARCH)){
            if(count > MAX_ROW_ID_CACHE_COUNT){
                loadRowRemap(position);
                int rowID = rowRemap.containsKey(position) ? rowRemap.get(position) : position+1;
                rule = databaseHelper.getSQLHandler(DNSRule.class).selectFirstRow(databaseHelper, false,
                        WhereCondition.equal(rowIDColumn, ""+rowID));
            }else{
                rule = databaseHelper.getSQLHandler(DNSRule.class).selectFirstRow(databaseHelper, false,
                        WhereCondition.equal(rowIDColumn, ""+ rows.get(position)));
            }
        }else {
            loadRowRemap(position);
            int rowID = rowRemap.containsKey(position) ? rowRemap.get(position) : position+1;
            rule = databaseHelper.getSQLHandler(DNSRule.class).selectFirstRow(databaseHelper, false,
                    WhereCondition.equal(rowIDColumn, "" + rowID));
        }
        ((TextView)holder.itemView.findViewById(R.id.text)).setText(rule.getHost());
        ((TextView)holder.itemView.findViewById(R.id.text3)).setText(rule.getTarget());
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new NewRuleDialog(context, new NewRuleDialog.CreationListener() {
                    @Override
                    public void creationFinished(@NonNull String host, @Nullable String target, @Nullable String targetV6, boolean ipv6, boolean wildcard, boolean editingMode) {
                        if(target != null) Util.getDBHelper(context).editDNSRule(host, ipv6, target);
                        else{
                            Util.getDBHelper(context).deleteDNSRule(host, ipv6);
                        }
                        reloadData();
                    }
                }, rule.getHost(), rule.getTarget(), rule.isWildcard(), rule.isIpv6()).show();
                return true;
            }
        });
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
        public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues);
    }

    public enum ArgumentLessFilter implements Filter{
        SHOW_IPV6 {
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                if(filterValues.containsKey(SHOW_IPV4))return null;
                return WhereCondition.equal(ipv6Column, "1");
            }
        }, HIDE_LOCAL {
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                return WhereCondition.equal(targetColumn, "127.0.0.1").not()
                        .and(WhereCondition.equal(targetColumn, "::1").not());
            }
        }, SHOW_WILDCARD {
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                if(filterValues.containsKey(SHOW_NORMAL))return null;
                return WhereCondition.equal(wildcardColumn, "1");
            }
        }, SHOW_IPV4 {
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                if(filterValues.containsKey(SHOW_IPV6))return null;
                return WhereCondition.equal(ipv6Column, "0");
            }
        }, SHOW_NORMAL{
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                if(filterValues.containsKey(SHOW_WILDCARD))return null;
                return WhereCondition.equal(wildcardColumn, "0");
            }
        }
    }

    public enum ArgumentBasedFilter implements Filter{
        TARGET {
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                return WhereCondition.like(targetColumn, "%" + argument + "%");
            }
        }, HOST_SEARCH{
            @Override
            public WhereCondition getCondition(String argument, HashMap<Filter, String> filterValues) {
                return WhereCondition.like(hostColumn, "%" + argument + "%");
            }
        }
    }
}
