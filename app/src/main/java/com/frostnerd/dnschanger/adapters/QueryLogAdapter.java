package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.database.DatabaseAdapter;
import com.frostnerd.database.orm.parser.columns.Column;
import com.frostnerd.database.orm.statementoptions.queryoptions.OrderOption;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.lifecycle.BaseViewHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
public class QueryLogAdapter extends DatabaseAdapter<DNSQuery, QueryLogAdapter.ViewHolder> {
    private final SimpleDateFormat timeFormatter,
            formatterDate, formatterDateYear, formatterYear = new SimpleDateFormat("yyyy");
    private final long dayStart = getStartOfDay(new Date()).getTime(),
            yearStart = getStartOfYear().getTime();
    private final boolean landscape;
    private static Column<DNSQuery> hostColumn;
    private LayoutInflater layoutInflater;

    public QueryLogAdapter(final @NonNull Context context, View progressView, final TextView rowCount) {
        super(DatabaseHelper.getInstance(context), 10000);
        this.layoutInflater = LayoutInflater.from(context);
        setOnRowLoaded(new OnRowLoaded<DNSQuery, ViewHolder>() {
            @Override
            public void bindRow(@NonNull ViewHolder view, @NonNull DNSQuery entity, int position) {
                String text;
                if (entity.getTime() < dayStart) {
                    if (entity.getTime() < yearStart) {
                        text = landscape ? formatterDateYear.format(entity.getTime()) : formatterYear.format(entity.getTime());
                    } else text = formatterDate.format(entity.getTime());
                } else text = timeFormatter.format(entity.getTime());
                view.time.setText(text);
                String hostText = entity.getHost();
                if(!TextUtils.isEmpty(entity.getUpstreamAnswer())) hostText += "\n -> " + entity.getUpstreamAnswer();
                view.host.setText(hostText);
            }

            @Override
            public void bindNonEntityRow(ViewHolder view, int position) {

            }
        });
        setReloadCallback(new Runnable() {
            final Handler main = new Handler(Looper.getMainLooper());
            @Override
            public void run() {
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        rowCount.setText(context.getString(R.string.x_entries).replace("[x]", getItemCount() + ""));
                    }
                });
            }
        });
        setProgressView(progressView);
        landscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        timeFormatter = new SimpleDateFormat("HH:mm:ss");
        if(landscape){
            formatterDate = new SimpleDateFormat("dd.MM HH:mm");
            formatterDateYear = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        }else{
            formatterDate = new SimpleDateFormat("dd.MM");
            formatterDateYear = new SimpleDateFormat("dd.MM.yy");
        }
        setOrderOption(new OrderOption(DatabaseHelper.getInstance(context).findColumn(DNSQuery.class, "time")).desc());
        hostColumn = DatabaseHelper.getInstance(context).findColumn(DNSQuery.class, "host");
        reloadData();
    }

    public void newQueryLogged(int rowID){
        insertAtFront(rowID, false);
    }

    private Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getStartOfYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.MONTH, 0);
        return calendar.getTime();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(layoutInflater.inflate(R.layout.row_query_log, parent, false));
    }

    static class ViewHolder extends BaseViewHolder {
        private TextView host, time;

        private ViewHolder(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.time);
            host = itemView.findViewById(R.id.host);
        }

        @Override
        protected void destroy() {
            host = time = null;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            host = time = null;
        }
    }

    @Override
    protected boolean beforeReload() {
        return true;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        layoutInflater = null;
    }

    public enum ArgumentBasedFilter implements ArgumentFilter{
        HOST_SEARCH{
            @Override
            public WhereCondition getCondition(String argument) {
                return WhereCondition.like(hostColumn, "%" + argument + "%");
            }

            @Override
            public boolean isResourceIntensive() {
                return true;
            }

            @Override
            public Filter[] exclusiveWith() {
                return new Filter[0];
            }
        }
    }
}
