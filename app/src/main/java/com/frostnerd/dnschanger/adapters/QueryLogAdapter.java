package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.utils.adapters.DatabaseAdapter;
import com.frostnerd.utils.database.orm.parser.columns.Column;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.OrderOption;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
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
            public void bindRow(ViewHolder view, DNSQuery entity, int position) {
                String text;
                if (entity.getTime() < dayStart) {
                    if (entity.getTime() < yearStart) {
                        text = landscape ? formatterDateYear.format(entity.getTime()) : formatterYear.format(entity.getTime());
                    } else text = formatterDate.format(entity.getTime());
                } else text = timeFormatter.format(entity.getTime());
                view.time.setText(text);
                view.host.setText(entity.getHost());
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView host, time;

        private ViewHolder(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.time);
            host = itemView.findViewById(R.id.host);
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
