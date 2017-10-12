package com.frostnerd.dnschanger.adapters;

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
    private boolean wildcard = false;
    private Context context;

    public RuleAdapter(Context context, DatabaseHelper databaseHelper){
        this.databaseHelper = databaseHelper;
        this.context = context;
        inflater = LayoutInflater.from(context);
        evaluateData();
    }

    public void search(String search){
        this.search = search;
        evaluateData();
        notifyDataSetChanged();
    }

    public void setWildcardMode(boolean wildcard, boolean resetSearch){
        if(resetSearch)search = "";
        this.wildcard = wildcard;
        evaluateData();
        notifyDataSetChanged();
    }

    public void reloadData(){
        evaluateData();
        notifyDataSetChanged();
    }

    private void evaluateData(){
        Cursor cursor;
        rows.clear();
        if(!search.equals("") || wildcard){
            if(!search.equals(""))cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT ROWID FROM DNSRules WHERE Domain LIKE '%" + search + "%' AND Wildcard=?", new String[]{wildcard ? "1" : "0"});
            else cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT ROWID FROM DNSRules WHERE Wildcard=?", new String[]{wildcard ? "1" : "0"});
            count = cursor.getCount();
            if(cursor.moveToFirst()){
                do{
                    rows.add((int)cursor.getLong(0));
                }while(cursor.moveToNext());
            }
            cursor.close();
        }else{
            cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT COUNT(Domain) FROM DNSRules WHERE Wildcard=?", new String[]{wildcard ? "1" : "0"});
            cursor.moveToFirst();
            count = cursor.getInt(0);
            cursor.close();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.row_rule, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Cursor cursor;
        if(!search.equals("") || wildcard)cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target FROM DNSRules WHERE ROWID=" + rows.get(position), null);
        else cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target, Wildcard FROM DNSRules WHERE ROWID=" + (position=position+1), null);
        final String host = cursor.getString(cursor.getColumnIndex("Domain")),
        target = cursor.getString(cursor.getColumnIndex("Target"));
        final boolean ipv6 = cursor.getInt(cursor.getColumnIndex("IPv6")) == 1,
        wildcard = cursor.getInt(cursor.getColumnIndex("Wildcard")) == 1;
        ((TextView)holder.itemView.findViewById(R.id.text)).setText(host);
        ((TextView)holder.itemView.findViewById(R.id.text2)).setText(ipv6 ? "✓" : "x");
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
}
