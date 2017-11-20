package com.frostnerd.dnschanger.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.RemoteViews;

import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class BasicWidget extends AppWidgetProvider {
    private static final String LOG_TAG = "[BasicWidget]";

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        LogFactory.writeMessage(context, LOG_TAG, "Updating " + appWidgetIds.length + " Widgets.");
        if (Util.isServiceRunning(context)) {
            LogFactory.writeMessage(context, LOG_TAG, "Waiting for broadcast...");
            LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context iContext, Intent intent) {
                    if (intent.getStringExtra("dns1") == null || intent.getStringExtra("dns1").equals("")) {
                        updateWidgetsNotRunning(context, appWidgetManager, appWidgetIds);
                    }else {
                        updateWidgets(context, appWidgetManager, appWidgetIds, intent.getStringExtra("dns1"),
                                intent.getStringExtra("dns2"), intent.getStringExtra("dns1v6"), intent.getStringExtra("dns2v6"));
                    }
                    LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                    LogFactory.writeMessage(context, LOG_TAG, appWidgetIds.length + " Widgets updated.");
                }
            }, new IntentFilter(Util.BROADCAST_SERVICE_STATUS_CHANGE));
        } else {
            LogFactory.writeMessage(context, LOG_TAG, "Service not running.");
            updateWidgetsNotRunning(context, appWidgetManager, appWidgetIds);
        }
    }

    private void updateWidgets(final Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, String dns1, String dns2, String dns1V6, String dns2V6) {
        RemoteViews views;
        for (int i = 0; i < appWidgetIds.length; i++) {
            views = new RemoteViews(context.getPackageName(), R.layout.widget_basic);
            views = resetWidget(views);
            views.setOnClickPendingIntent(R.id.basic_widget, PendingIntent.getActivity(context, 0, new Intent(context, PinActivity.class).putExtra("main", true), 0));
            views.setTextViewText(R.id.dns1, dns1);
            views.setTextViewText(R.id.dns2, dns2);
            views.setTextViewText(R.id.dns1_v6, dns1V6);
            views.setTextViewText(R.id.dns2_v6, dns2V6);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
        }
    }

    private RemoteViews resetWidget(RemoteViews views) {
        views.setTextViewText(R.id.head, "");
        views.setViewVisibility(R.id.head, View.GONE);
        views.setViewVisibility(R.id.dns1_wrap, View.VISIBLE);
        views.setViewVisibility(R.id.dns2_wrap, View.VISIBLE);
        views.setViewVisibility(R.id.dns1v6_wrap, View.VISIBLE);
        views.setViewVisibility(R.id.dns2v6_wrap, View.VISIBLE);
        return views;
    }

    private void updateWidgetsNotRunning(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        RemoteViews views;
        for (int i = 0; i < appWidgetIds.length; i++) {
            views = new RemoteViews(context.getPackageName(), R.layout.widget_basic);
            views.setOnClickPendingIntent(R.id.basic_widget, PendingIntent.getActivity(context, 0, new Intent(context, PinActivity.class).putExtra("main", true), 0));
            views.setTextViewText(R.id.head, context.getString(R.string.widget_not_running));
            views.setViewVisibility(R.id.head, View.VISIBLE);
            views.setViewVisibility(R.id.dns1_wrap, View.GONE);
            views.setViewVisibility(R.id.dns2_wrap, View.GONE);
            views.setViewVisibility(R.id.dns1v6_wrap, View.GONE);
            views.setViewVisibility(R.id.dns2v6_wrap, View.GONE);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
        }
    }
}
