package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.R;

import org.xbill.DNS.DClass;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class QueryResultAdapter extends RecyclerView.Adapter<QueryResultAdapter.ViewHolder> {
    private RRset[] answer, authority, additional;
    private Context context;
    private LayoutInflater layoutInflater;
    private List<Entry> entryList = new ArrayList<>();

    public QueryResultAdapter(Context context, RRset[] answer, RRset[] authority, RRset[] additional){
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        this.answer = answer;
        this.authority = authority;
        this.additional = additional;
        populateEntryList();
    }

    private void populateEntryList(){
        for(RRset rset: answer){
            for(Iterator<Record> it = rset.rrs(); it.hasNext();){
                entryList.add(new Entry(rset, it.next()));
            }
        }
    }

    @Override
    public QueryResultAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(layoutInflater.inflate(R.layout.row_dns_query, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public void onBindViewHolder(QueryResultAdapter.ViewHolder holder, int position) {
        if(holder.getItemViewType() == 0){
            holder.contentView.setBackgroundColor(ThemeHandler.getColor(context, R.attr.cardColor, -1));
            ((TextView)holder.contentView.findViewById(R.id.text1)).setText(R.string.query_title_name);
            ((TextView)holder.contentView.findViewById(R.id.text2)).setText(R.string.query_title_ttl);
            ((TextView)holder.contentView.findViewById(R.id.text3)).setText(R.string.query_title_dclass);
            ((TextView)holder.contentView.findViewById(R.id.text4)).setText(R.string.query_title_type);
            ((TextView)holder.contentView.findViewById(R.id.text5)).setText(R.string.query_title_answer);

            ((TextView)holder.contentView.findViewById(R.id.text1)).setTextColor(Color.parseColor("#FFFFFF"));
            ((TextView)holder.contentView.findViewById(R.id.text2)).setTextColor(Color.parseColor("#FFFFFF"));
            ((TextView)holder.contentView.findViewById(R.id.text3)).setTextColor(Color.parseColor("#FFFFFF"));
            ((TextView)holder.contentView.findViewById(R.id.text4)).setTextColor(Color.parseColor("#FFFFFF"));
            ((TextView)holder.contentView.findViewById(R.id.text5)).setTextColor(Color.parseColor("#FFFFFF"));
        }else{
            position = position - 1;
            RRset rset = entryList.get(position).rset;
            Record record = entryList.get(position).record;
            ((TextView)holder.contentView.findViewById(R.id.text1)).setText(rset.getName().toString());
            ((TextView)holder.contentView.findViewById(R.id.text2)).setText(rset.getTTL() + "");
            ((TextView)holder.contentView.findViewById(R.id.text3)).setText(DClass.string(rset.getDClass()));
            ((TextView)holder.contentView.findViewById(R.id.text4)).setText(Type.string(rset.getType()));
            ((TextView)holder.contentView.findViewById(R.id.text5)).setText(record.rdataToString());
        }
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private View contentView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.contentView = itemView;
        }
    }

    private class Entry{
        private RRset rset;
        private Record record;

        public Entry(RRset rset, Record record) {
            this.rset = rset;
            this.record = record;
        }
    }
}
