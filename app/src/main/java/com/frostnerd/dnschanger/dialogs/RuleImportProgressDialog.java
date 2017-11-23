package com.frostnerd.dnschanger.dialogs;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.fragments.RulesFragment;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RuleImportProgressDialog extends AlertDialog {
    private static final Pattern DNSMASQ_PATTERN = Pattern.compile("^address=/([^/]+)/(?:([0-9.]+)|([0-9a-fA-F:]+))");
    private static final Matcher DNSMASQ_MATCHER = DNSMASQ_PATTERN.matcher("");
    private static final Pattern HOSTS_PATTERN = Pattern.compile("^(?:([^#\\s]+)\\s+(((?:[0-9.[^#\\s]])+$)|(?:[0-9a-fA-F:[^#\\s]]+)))|(?:^(?:([0-9.]+)|([0-9a-fA-F:]+))\\s+([^#\\s]+))");
    private static final Matcher HOSTS_MATCHER = HOSTS_PATTERN.matcher("");
    private static final Pattern DOMAINS_PATTERN = Pattern.compile("^([A-Za-z0-9][A-Za-z0-9\\-.]+)");
    private static final Matcher DOMAINS_MATCHER = DOMAINS_PATTERN.matcher("");
    private static final Pattern ADBLOCK_PATTERN = Pattern.compile("^\\|\\|([A-Za-z0-9][A-Za-z0-9\\-.]+)\\^");
    private static final Matcher ADBLOCK_MATCHER = ADBLOCK_PATTERN.matcher("");
    private int linesCombined;
    private final TextView progressText;
    private final TextView fileText;
    private List<ImportableFile> files;
    private AsyncImport asyncImport;

    public RuleImportProgressDialog(@NonNull Activity context, List<ImportableFile> files, int databaseConflictHandling) {
        super(context, ThemeHandler.getDialogTheme(context));
        this.files = files;
        for(ImportableFile file: files)linesCombined += file.getLines();
        setTitle(getContext().getString(R.string.importing_x_rules).replace("[x]", "" + linesCombined));
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setButton(BUTTON_NEUTRAL, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                asyncImport.cancel(false);
                asyncImport = null;
                dialog.dismiss();
            }
        });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(asyncImport != null)asyncImport.cancel(false);
                asyncImport = null;
            }
        });
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_rule_import_progress, null, false));
        progressText = content.findViewById(R.id.progress_text);
        fileText = content.findViewById(R.id.file_name);
        asyncImport = new AsyncImport(context, this, files, databaseConflictHandling, linesCombined);
        asyncImport.execute();
    }

    @Override
    public void dismiss() {
        asyncImport = null;
        files = null;
        super.dismiss();
    }

    public void setHostingActivityPaused(boolean paused){
        if(asyncImport != null)asyncImport.setPaused(paused);
    }

    public static int getFileLines(File f) {
        int lines = 0;
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(f));
            lnr.skip(Long.MAX_VALUE);
            lines = lnr.getLineNumber() + 1;
            lnr.close();
        } catch (IOException ignored) {

        }
        return lines;
    }

    public static FileType tryFindFileType(File f, boolean failFast){
        try{
            HashMap<FileType, Integer> validLines = new LinkedHashMap<>();
            for(FileType type: FileType.values())validLines.put(type, 0);
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            int lines = 0, fileLines = failFast ? 0 : getFileLines(f);
            FileType won = null;
            while((line = reader.readLine()) != null && ((failFast && lines++ <= 300) || (!failFast && lines++ <= fileLines))){
                for(FileType type: validLines.keySet()){
                    if(type.parseLine(line) != null){
                        validLines.put(type, validLines.get(type)+1);
                        if(validLines.get(type) >= 50){
                            won = type;
                            break;
                        }
                    }
                }
            }
            reader.close();
            if(won == null){
                Map.Entry<FileType, Integer> max = null;
                for(Map.Entry<FileType, Integer> entry: validLines.entrySet()){
                    if(max == null || entry.getValue().compareTo(max.getValue()) > 0)max = entry;
                }
                if(max != null && ((double)max.getValue()/lines) >= 0.66)won = max.getKey();
            }
            return won;
        }catch (IOException ignored){

        }
        return null;
    }

    public enum FileType implements LineParser {
        DNSMASQ {
            @Override
            public TemporaryDNSRule parseLine(String line) {
                if(DNSMASQ_MATCHER.reset(line).find()){
                    String host = DNSMASQ_MATCHER.group(1);
                    String target = DNSMASQ_MATCHER.group(2);
                    if(target != null && NetworkUtil.isIP(target, false)){
                        if(target.equals("0.0.0.0"))return new TemporaryDNSRule(host);
                        else return new TemporaryDNSRule(host, target, false);
                    }else if((target = DNSMASQ_MATCHER.group(3)) != null && NetworkUtil.isIP(target, true)){
                        return new TemporaryDNSRule(host, target, true);
                    }
                }
                return null;
            }
        }, HOST {
            @Override
            public TemporaryDNSRule parseLine(String line) {
                if(HOSTS_MATCHER.reset(line).find()){
                    String host = HOSTS_MATCHER.group(1), target;
                    boolean ipv6 = false;
                    if(NetworkUtil.isIPv4(host) || (ipv6 = NetworkUtil.isIP(host, true))){
                        target = host;
                        host = HOSTS_MATCHER.group(2);
                    }else{
                        target = HOSTS_MATCHER.group(2);
                        ipv6 = NetworkUtil.isIP(target, true);
                    }
                    if(!ipv6 && target.equals("0.0.0.0"))return new TemporaryDNSRule(host);
                    else if(NetworkUtil.isIP(target, ipv6))return new TemporaryDNSRule(host, target, ipv6);
                }
                return null;
            }
        }, ADBLOCK_FILE{
            @Override
            public TemporaryDNSRule parseLine(String line) {
                if(ADBLOCK_MATCHER.reset(line).find()){
                    String host = ADBLOCK_MATCHER.group(1);
                    return new TemporaryDNSRule(host);
                }
                return null;
            }
        }, DOMAIN_LIST {
            @Override
            public TemporaryDNSRule parseLine(String line) {
                if(DOMAINS_MATCHER.reset(line).find()){
                    String host = DOMAINS_MATCHER.group(1);
                    return new TemporaryDNSRule(host);
                }
                return null;
            }
        }

    }

    private interface LineParser {
        TemporaryDNSRule parseLine(String line);
    }

    private static class TemporaryDNSRule {
        final String host;
        String target;
        boolean ipv6, both = false;

        public TemporaryDNSRule(String host){
            this.host = host;
            both = true;
        }

        public TemporaryDNSRule(String host, String target, boolean IPv6) {
            this.host = host;
            this.target = target;
            this.ipv6 = IPv6;
        }
    }

    public static class ImportableFile{
        private final File file;
        private final FileType fileType;
        private final int lines;

        public ImportableFile(File file, FileType fileType, int lines) {
            this.file = file;
            this.fileType = fileType;
            this.lines = lines;
        }

        public File getFile() {
            return file;
        }

        public FileType getFileType() {
            return fileType;
        }

        public int getLines() {
            return lines;
        }
    }

    private static class AsyncImport extends AsyncTask<Void, Integer, Void>{
        private int validLines = 0, distinctEntries = 0;
        private final int NOTIFICATION_ID = 655;
        private List<ImportableFile> files;
        private Context context;
        private final int databaseConflictHandling;
        private final int linesCombined;
        private RuleImportProgressDialog dialog;
        private NotificationCompat.Builder notificationBuilder;
        private NotificationManager notificationManager;
        private final int notificationUpdateCount;
        private int lastNotificationUpdate = -1;
        private boolean paused = false;

        public AsyncImport(Context context, RuleImportProgressDialog dialog, List<ImportableFile> files, int databaseConflictHandling, int linesCombined){
            this.context = dialog.getContext();
            this.files = files;
            this.databaseConflictHandling = databaseConflictHandling;
            this.linesCombined = linesCombined;
            this.dialog = dialog;
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationBuilder = new NotificationCompat.Builder(context, Util.createNotificationChannel(context, false));
            notificationBuilder.setSmallIcon(R.drawable.ic_action_import);
            notificationBuilder.setContentTitle(context.getString(R.string.importing_x_rules).replace("[x]", ""+linesCombined));
            notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, PinActivity.class), 0));
            notificationBuilder.setAutoCancel(false);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setUsesChronometer(true);
            notificationBuilder.setColorized(false);
            notificationBuilder.setSound(null);
            if(linesCombined >= 750000){
                notificationUpdateCount = linesCombined/110;
            }else if(linesCombined >= 500000){
                notificationUpdateCount = linesCombined/90;
            }else if(linesCombined >= 250000){
                notificationUpdateCount = linesCombined/70;
            }else if(linesCombined >= 100000){
                notificationUpdateCount = linesCombined/50;
            }else if(linesCombined >= 50000){
                notificationUpdateCount = linesCombined/40;
            }else if(linesCombined >= 10000){
                notificationUpdateCount = linesCombined/30;
            }else if(linesCombined >= 100){
                notificationUpdateCount = linesCombined/20;
            }else notificationUpdateCount = 5;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                startImport();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void startImport() throws IOException {
            SQLiteDatabase database = Util.getDBHelper(context).getWritableDatabase();
            database.beginTransaction();
            String line;
            TemporaryDNSRule rule;
            ContentValues values = new ContentValues(3);
            int i = 0, pos = 0, currentCount, rowID;
            String ruleTableName = Util.getDBHelper(context).getTableName(DNSRule.class),
                columnHost = Util.getDBHelper(context).findColumn(DNSRule.class, "host").getColumnName(),
                columnTarget = Util.getDBHelper(context).findColumn(DNSRule.class, "target").getColumnName(),
                columnIPv6 = Util.getDBHelper(context).findColumn(DNSRule.class, "ipv6").getColumnName();
            for(ImportableFile file: files){
                currentCount = 0;
                BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
                LineParser parser = file.getFileType();
                onProgressUpdate(-1, pos++);
                rowID = Util.getDBHelper(context).getHighestRowID(DNSRule.class);
                while (!isCancelled() && (line = reader.readLine()) != null) {
                    i++;
                    rule = parser.parseLine(line.trim());
                    if (rule != null) {
                        validLines++;
                        values.put(columnHost, rule.host);
                        if(rule.both){
                            values.put(columnTarget, "127.0.0.1");
                            values.put(columnIPv6, false);
                            if(database.insertWithOnConflict(ruleTableName, null, values, databaseConflictHandling) != -1){
                                distinctEntries++;
                                currentCount++;
                            }
                            values.put(columnTarget, "::1");
                            values.put(columnIPv6, true);
                        }else{
                            values.put(columnTarget, rule.target);
                            values.put(columnIPv6, rule.ipv6);
                        }
                        if(database.insertWithOnConflict(ruleTableName, null, values, databaseConflictHandling) != -1){
                            distinctEntries++;
                            currentCount++;
                        }
                        values.clear();
                    }
                    publishProgress(i);
                }
                if(!isCancelled() && currentCount != 0) Util.getDBHelper(context).insert(new DNSRuleImport(file.getFile().getName(), System.currentTimeMillis(),
                        Util.getDBHelper(context).getByRowID(DNSRule.class, rowID),
                        Util.getDBHelper(context).getLastRow(DNSRule.class)));
                reader.close();
            }
            if (!isCancelled()) database.setTransactionSuccessful();
            database.endTransaction();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            cleanup();
            new AlertDialog.Builder(context, ThemeHandler.getDialogTheme(context)).setTitle(R.string.done).setCancelable(true).
                    setNeutralButton(R.string.close, null).
                    setMessage(context.getString(R.string.rules_import_finished).
                            replace("[x]", "" + linesCombined).replace("[y]", "" + validLines).
                            replace("[z]", "" + distinctEntries)).show();
            dialog.dismiss();
            if(context instanceof MainActivity){
                Fragment fragment = ((MainActivity)context).currentFragment();
                if(fragment instanceof RulesFragment){
                    ((RulesFragment)fragment).getRuleAdapter().reloadData();
                }
            }
        }

        @Override
        protected void onCancelled() {
            cleanup();
        }

        private void cleanup(){
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager = null;
            notificationBuilder = null;
            context = null;
            dialog = null;
            files = null;
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            if(values.length == 2){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.fileText.setText(files.get(values[1]).getFile().getName());
                    }
                });
            } else {
                int i = values[0];
                if(!paused)dialog.progressText.setText(i + "/" + linesCombined);
                if(i > lastNotificationUpdate){
                    lastNotificationUpdate += notificationUpdateCount;
                    notificationBuilder.setContentText(i + "/" + linesCombined);
                    notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
                }
            }
        }

    }
}
