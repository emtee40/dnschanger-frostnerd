package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.adapters.BaseAdapter;
import com.frostnerd.utils.adapters.BaseViewHolder;
import com.frostnerd.utils.general.DesignUtil;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

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
public class QueryResultAdapter extends BaseAdapter<QueryResultAdapter.ViewHolder> {
    private Record[] answer;
    private Context context;
    private LayoutInflater layoutInflater;

    public QueryResultAdapter(Context context, Record[] answer){
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        this.answer = answer;
    }

    @Override
    protected void cleanup(){
        context = null;
        layoutInflater = null;
        answer = null;
    }

    @NonNull
    @Override
    public QueryResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(answer.length, context, (LinearLayout)layoutInflater.inflate(R.layout.row_dns_query, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull QueryResultAdapter.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        LinearLayout layout = (LinearLayout) holder.itemView;
        int titleText;
        if (position == 0) titleText = R.string.query_title_name;
        else if (position == 1) titleText = R.string.query_title_ttl;
        else if (position == 2) titleText = R.string.query_title_dclass;
        else if (position == 3) titleText = R.string.query_title_type;
        else titleText = R.string.query_title_answer;
        int padding = (int) DesignUtil.dpToPixels(8, context), paddingLeft = position == 0 ? 0 : padding,
                paddingRight = position == getItemCount() - 1 ? 0 : padding;
        TextView textView = (TextView) layout.getChildAt(0);

        textView.setText(titleText);
        textView.setTextColor(Color.parseColor("#FFFFFF"));
        textView.setPadding(paddingLeft, 0, paddingRight, 0);
        textView.setTypeface(null, Typeface.BOLD);

        for (int i = 1; i < layout.getChildCount(); i++) {
            textView = ((TextView) layout.getChildAt(i));
            textView.setText(getText(position, i));
            textView.setPadding(paddingLeft, padding, paddingRight, 0);
        }
    }

    private String getText(int position, int index){
        if(position == 0)return answer[index-1].getName().toString();
        else if(position == 1)return String.valueOf(answer[index-1].getTTL());
        else if(position == 2)return DClass.string(answer[index-1].getDClass());
        else if(position == 3)return Type.string(answer[index-1].getType());
        else return answer[index-1].rdataToString();
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    static class ViewHolder extends BaseViewHolder{
        private ViewHolder(int elementCount, Context context, LinearLayout itemView) {
            super(itemView);
            TextView text;
            for(int i = 0; i <= elementCount; i++){
                text = new TextView(context);
                itemView.addView(text);
            }
        }

        @Override
        protected void destroy() {

        }
    }
}
