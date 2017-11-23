package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.adapters.DatabaseAdapter;
import com.frostnerd.utils.database.orm.parser.Column;
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
public class QueryLogAdapter extends DatabaseAdapter<DNSQuery> {
    private final SimpleDateFormat timeFormatter,
            formatterDate, formatterDateYear, formatterYear = new SimpleDateFormat("yyyy");
    private final long dayStart = getStartOfDay(new Date()).getTime(),
            yearStart = getStartOfYear().getTime();
    private final boolean landscape;
    private static Column<DNSQuery> hostColumn;


    public QueryLogAdapter(final @NonNull Context context, View progressView, final TextView rowCount) {
        super(context, Util.getDBHelper(context), R.layout.row_query_log, 10000);
        setOnRowLoaded(new OnRowLoaded<DNSQuery>() {
            @Override
            public void bindRow(View view, DNSQuery entity) {
                String text;
                if (entity.getTime() < dayStart) {
                    if (entity.getTime() < yearStart) {
                        text = landscape ? formatterDateYear.format(entity.getTime()) : formatterYear.format(entity.getTime());
                    } else text = formatterDate.format(entity.getTime());
                } else text = timeFormatter.format(entity.getTime());
                ((TextView) view.findViewById(R.id.time)).setText(text);
                ((TextView) view.findViewById(R.id.host)).setText(entity.getHost());
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
        setOrderOption(new OrderOption(Util.getDBHelper(context).findColumn(DNSQuery.class, "time")).desc());
        hostColumn = Util.getDBHelper(context).findColumn(DNSQuery.class, "host");
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