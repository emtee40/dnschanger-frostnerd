package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.DatabaseHelper;

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

    public RuleAdapter(Context context, DatabaseHelper databaseHelper){
        this.databaseHelper = databaseHelper;
        inflater = LayoutInflater.from(context);
        evaluateCount();
    }

    public void search(String search){
        this.search = search;
        evaluateCount();
        notifyDataSetChanged();
    }

    private void evaluateCount(){
        Cursor cursor;
        if(!search.equals(""))cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT COUNT(Domain) FROM DNSRules WHERE Domain LIKE '%" + search + "%'", null);
        else cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT COUNT(Domain) FROM DNSRules", null);
        cursor.moveToFirst();
        count = cursor.getInt(0);
        cursor.close();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.row_rule, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Cursor cursor;
        if(!search.equals(""))cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target FROM DNSRules WHERE Domain LIKE '%" + search + "%' LIMIT " + position + ",1" , null);
        else cursor = databaseHelper.getReadableDatabase().rawQuery("SELECT Domain, IPv6, Target FROM DNSRules LIMIT " + position + ",1", null);
        cursor.moveToFirst();
        ((TextView)holder.itemView.findViewById(R.id.text)).setText(cursor.getString(cursor.getColumnIndex("Domain")));
        ((TextView)holder.itemView.findViewById(R.id.text2)).setText(cursor.getInt(cursor.getColumnIndex("IPv6")) == 1 ? "✓" : "x");
        ((TextView)holder.itemView.findViewById(R.id.text3)).setText(cursor.getString(cursor.getColumnIndex("Target")));
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
