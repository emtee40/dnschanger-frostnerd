package com.frostnerd.dnschanger.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.util.RuleImport;
import com.frostnerd.dnschanger.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RuleImportService extends Service {
    public static final String PARAM_FILE_LIST = "filelist",
            PARAM_LINE_COUNT = "lineCount",
            PARAM_DATABASE_CONFLICT_HANDLING = "conflictHandling",
            BROADCAST_EVENT_DATABASE_UPDATED = "com.frostnerd.dnschanger.RULE_DATABASE_UPDATE";
    private static final int NOTIFICATION_ID = 655;
    private int NOTIFICATION_ID_FINISHED = NOTIFICATION_ID+1;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder, notificationBuilderFinished;
    private int lastNotificationUpdate = -1;
    private int notificationUpdateCount;
    private Deque<Configuration> configurations;
    private boolean shouldContinue = true;

    public static Intent createIntent(Context context, int lineCount, int databaseConflictHandling, RuleImport.ImportableFile... importableFiles){
        Intent intent = new Intent(context, RuleImportService.class);
        intent.putExtra(PARAM_LINE_COUNT, lineCount);
        intent.putExtra(PARAM_DATABASE_CONFLICT_HANDLING, databaseConflictHandling);
        intent.putExtra(PARAM_FILE_LIST, FileList.of(importableFiles));
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isValidIntent(intent)) {
            boolean first = configurations == null;
            if(first)configurations = new ArrayDeque<>();
            final FileList list = (FileList) intent.getSerializableExtra(PARAM_FILE_LIST);
            final int conflictHandling = intent.getIntExtra(PARAM_DATABASE_CONFLICT_HANDLING, SQLiteDatabase.CONFLICT_ABORT);
            initNotification();
            configurations.add(new Configuration(list, intent.getIntExtra(PARAM_LINE_COUNT, -1), conflictHandling));
            if(first)new Thread(){
                @Override
                public void run() {
                    try {
                        startImport();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            return START_STICKY;
        }else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void startImport() throws IOException {
        String line;
        RuleImport.TemporaryDNSRule rule;
        ContentValues values = new ContentValues(4);
        int i = 0, currentCount, rowID;
        String ruleTableName = Util.getDBHelper(this, false).getTableName(DNSRule.class),
                columnHost = Util.getDBHelper(this, false).findColumn(DNSRule.class, "host").getColumnName(),
                columnTarget = Util.getDBHelper(this, false).findColumn(DNSRule.class, "target").getColumnName(),
                columnIPv6 = Util.getDBHelper(this, false).findColumn(DNSRule.class, "ipv6").getColumnName(),
        columnWildcard = Util.getDBHelper(this, false).findColumn(DNSRule.class, "wildcard").getColumnName();
        values.put(columnWildcard, "0");
        while(shouldContinue && configurations.size() != 0){
            Configuration configuration = configurations.removeFirst();
            determineNotificationUpdateCount(configuration.lineCount);
            updateNotification(configuration.lineCount);
            int validLines = 0, distinctEntries = 0;
            SQLiteDatabase database = Util.getDBHelper(this, false).getWritableDatabase();
            database.beginTransaction();
            for(RuleImport.ImportableFile file: configuration.fileList.files){
                if(!shouldContinue)break;
                currentCount = 0;
                BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
                RuleImport.LineParser parser = file.getFileType();
                updateNotification(file.getFile());
                rowID = Util.getDBHelper(this, false).getHighestRowID(DNSRule.class);
                while (shouldContinue && (line = reader.readLine()) != null) {
                    i++;
                    rule = parser.parseLine(line.trim());
                    if (rule != null) {
                        validLines++;
                        values.put(columnHost, rule.getHost());
                        if(rule.isBoth()){
                            values.put(columnTarget, "127.0.0.1");
                            values.put(columnIPv6, false);
                            if(database.insertWithOnConflict(ruleTableName, null, values, configuration.databaseConflictHandling) != -1){
                                distinctEntries++;
                                currentCount++;
                            }
                            values.put(columnTarget, "::1");
                            values.put(columnIPv6, true);
                        }else{
                            values.put(columnTarget, rule.getTarget());
                            values.put(columnIPv6, rule.isIpv6());
                        }
                        if(database.insertWithOnConflict(ruleTableName, null, values, configuration.databaseConflictHandling) != -1){
                            distinctEntries++;
                            currentCount++;
                        }
                    }
                    updateNotification(i, configuration.lineCount);
                }
                if(shouldContinue && currentCount != 0) Util.getDBHelper(this, false).insert(new DNSRuleImport(file.getFile().getName(), System.currentTimeMillis(),
                        Util.getDBHelper(this, false).getByRowID(DNSRule.class, rowID),
                        Util.getDBHelper(this, false).getLastRow(DNSRule.class)));
                reader.close();
            }
            String text;
            notificationBuilderFinished.setContentText(text = getString(R.string.rules_import_finished).
                    replace("[x]", configuration.lineCount + "").
                    replace("[y]", validLines + "").
                    replace("[z]", distinctEntries + ""));
            notificationBuilderFinished.setContentTitle(getString(R.string.done));
            notificationBuilderFinished.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            notificationManager.notify(NOTIFICATION_ID_FINISHED++, notificationBuilderFinished.build());
            if (shouldContinue) database.setTransactionSuccessful();
            database.endTransaction();
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(BROADCAST_EVENT_DATABASE_UPDATED));
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //super.onTaskRemoved(rootIntent);
        //cleanup();
    }

    private void cleanup() {
        if (notificationManager == null) return;
        shouldContinue = false;
        notificationManager.cancel(NOTIFICATION_ID);
        notificationManager = null;
        notificationBuilder = null;
        configurations.clear();
    }

    private void updateNotification(File file){
        notificationBuilder.setSubText(file.getName());
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(int currentLines, int lineCount){
        if(currentLines > lastNotificationUpdate){
            lastNotificationUpdate += notificationUpdateCount;
            notificationBuilder.setContentText(currentLines + "/" + lineCount);
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void updateNotification(int combinedLineCount){
        notificationBuilder.setContentTitle(getString(R.string.importing_x_rules).replace("[x]", "" + combinedLineCount));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void initNotification() {
        if(notificationManager != null)return;
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(this, Util.createNotificationChannel(this, false));
        notificationBuilder.setSmallIcon(R.drawable.ic_action_import);
        notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, PinActivity.class), 0));
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setUsesChronometer(true);
        notificationBuilder.setColorized(false);
        notificationBuilder.setSound(null);

        notificationBuilderFinished = new NotificationCompat.Builder(this, Util.createNotificationChannel(this, false));
        notificationBuilderFinished.setSmallIcon(R.drawable.ic_action_import);
        notificationBuilderFinished.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, PinActivity.class), 0));
        notificationBuilderFinished.setAutoCancel(true);
        notificationBuilderFinished.setOngoing(false);
        notificationBuilderFinished.setUsesChronometer(false);
        notificationBuilderFinished.setColorized(false);
        notificationBuilderFinished.setSound(null);
    }

    private void determineNotificationUpdateCount(int combinedLineCount){
        int factor = -1;
        if (combinedLineCount >= 750000) factor = 210;
        else if (combinedLineCount >= 500000) factor = 150;
        else if (combinedLineCount >= 250000) factor = 105;
        else if (combinedLineCount >= 100000) factor = 65;
        else if (combinedLineCount >= 50000) factor = 45;
        else if (combinedLineCount >= 10000) factor = 30;
        else if (combinedLineCount >= 100) factor = 20;
        if(combinedLineCount >= 1500000)factor = (int)(factor*1.7);
        notificationUpdateCount = factor == -1 ? 5 : combinedLineCount/factor;
    }

    private boolean isValidIntent(Intent intent) {
        return intent != null && intent.getExtras() != null && intent.hasExtra(PARAM_FILE_LIST) &&
                intent.hasExtra(PARAM_LINE_COUNT) && intent.hasExtra(PARAM_DATABASE_CONFLICT_HANDLING);
    }

    public static final class FileList implements Serializable {
        private RuleImport.ImportableFile[] files;

        private FileList(RuleImport.ImportableFile[] files) {
            this.files = files;
        }

        public static FileList of(RuleImport.ImportableFile... files) {
            return new FileList(files);
        }
    }

    private class Configuration{
        private FileList fileList;
        private int lineCount;
        private int databaseConflictHandling;

        public Configuration(FileList fileList, int lineCount, int databaseConflictHandling) {
            this.fileList = fileList;
            this.lineCount = lineCount;
            this.databaseConflictHandling = databaseConflictHandling;
        }
    }
}
