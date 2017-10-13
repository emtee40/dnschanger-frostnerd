package com.frostnerd.dnschanger.dialogs;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.fragments.RulesFragment;
import com.frostnerd.dnschanger.util.API;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
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
    private static final Pattern DNSMASQ_PATTERN = Pattern.compile("^address=/([^/]+)/(?:([0-9.]+$)|([0-9a-fA-F:]+$))");
    private static final Matcher DNSMASQ_MATCHER = DNSMASQ_PATTERN.matcher("");
    private static final Pattern HOSTS_PATTERN = Pattern.compile("^(?:([^#\\s]+)\\s(((?:[0-9.^#\\s])+$)|(?:[0-9a-fA-F:^#\\s]+$)))|(?:^(?:([0-9.]+)|([0-9a-fA-F:]+))\\s([^#\\s]+$))");
    private static final Matcher HOSTS_MATCHER = HOSTS_PATTERN.matcher("");
    private static final Pattern DOMAINS_PATTERN = Pattern.compile("^([^#\\s=$%*/]+$)");
    private static final Matcher DOMAINS_MATCHER = DOMAINS_PATTERN.matcher("");
    private int lines;
    private LineParser parser;
    private Activity context;
    private TextView progressText;
    private AsyncTask<File, Integer, Void> asyncImport = new AsyncTask<File, Integer, Void>() {
        private int validLines = 0;

        @Override
        protected Void doInBackground(File... params) {
            try {
                startImport(params[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void startImport(File f) throws IOException {
            SQLiteDatabase database = API.getDBHelper(getContext()).getWritableDatabase();
            database.beginTransaction();
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            DNSRule rule;
            ContentValues values = new ContentValues(3);
            int i = 0;
            while (!isCancelled() && (line = reader.readLine()) != null) {
                i++;
                rule = parser.parseLine(line.trim());
                if (rule != null) {
                    validLines++;
                    values.put("Domain", rule.host);
                    if(rule.both){
                        values.put("Target", "127.0.0.1");
                        values.put("IPv6", false);
                        database.insertWithOnConflict("DNSRules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                        values.put("Target", "::1");
                        values.put("IPv6", true);
                    }else{
                        values.put("Target", rule.target);
                        values.put("IPv6", rule.ipv6);
                    }
                    database.insertWithOnConflict("DNSRules", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    values.clear();
                }
                publishProgress(i);
            }
            if (!isCancelled()) database.setTransactionSuccessful();
            database.endTransaction();
            if(context instanceof MainActivity){
                Fragment fragment = ((MainActivity)context).currentFragment();
                if(fragment instanceof RulesFragment){
                    ((RulesFragment)fragment).getRuleAdapter().reloadData();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext())).setTitle(R.string.done).setCancelable(true).
                    setNeutralButton(R.string.close, null).
                    setMessage(getContext().getString(R.string.rules_import_finished).replace("[x]", "" + lines).replace("[y]", "" + validLines)).show();
            dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int i = values[0];
            progressText.setText(i + "/" + lines);
        }
    };

    public RuleImportProgressDialog(@NonNull Activity context, File file, FileType type) {
        super(context, ThemeHandler.getDialogTheme(context));
        this.context = context;
        lines = getFileLines(file);
        setTitle(getContext().getString(R.string.importing_x_rules).replace("[x]", "" + lines));
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setButton(BUTTON_NEUTRAL, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                asyncImport.cancel(false);
                dialog.dismiss();
            }
        });
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_rule_import_progress, null, false));
        progressText = content.findViewById(R.id.progress_text);
        parser = type;
        asyncImport.execute(file);
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

    public static FileType tryFindFileType(File f){
        try{
            HashMap<FileType, Integer> validLines = new HashMap<>();
            for(FileType type: FileType.values())validLines.put(type, 0);
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            int lines = 0;
            FileType won = null;
            while((line = reader.readLine()) != null && lines++ <= 300){
                for(FileType type: validLines.keySet()){
                    if(type.parseLine(line) != null)validLines.put(type, validLines.get(type)+1);
                    if(validLines.get(type) >= 50){
                        won = type;
                        break;
                    }
                }
            }
            reader.close();
            return won;
        }catch (IOException ignored){

        }
        return null;
    }

    public enum FileType implements LineParser {
        DNSMASQ {
            @Override
            public DNSRule parseLine(String line) {
                if(DNSMASQ_MATCHER.reset(line).find()){
                    String host = DNSMASQ_MATCHER.group(1);
                    String target = DNSMASQ_MATCHER.group(2);
                    if(target != null && NetworkUtil.isIP(target, false)){
                        return new DNSRule(host, target, false);
                    }else if((target = DNSMASQ_MATCHER.group(3)) != null && NetworkUtil.isIP(target, true)){
                        return new DNSRule(host, target, true);
                    }
                }
                return null;
            }
        }, HOST {
            @Override
            public DNSRule parseLine(String line) {
                if(HOSTS_MATCHER.reset(line).find()){
                    String host = HOSTS_MATCHER.group(1), target;
                    boolean ipv6 = false;
                    if(host == null){
                        host = HOSTS_MATCHER.group(6);
                        target = HOSTS_MATCHER.group(4);
                        if(target == null){
                            ipv6 = true;
                            target = HOSTS_MATCHER.group(5);
                        }
                    }else{
                        target = HOSTS_MATCHER.group(3);
                        if(target == null){
                            ipv6 = true;
                            target = HOSTS_MATCHER.group(2);
                        }
                    }
                    if(NetworkUtil.isIP(target, ipv6))return new DNSRule(host, target, ipv6);
                }
                return null;
            }
        }, DOMAIN_LIST {
            @Override
            public DNSRule parseLine(String line) {
                if(DOMAINS_MATCHER.reset(line).find()){
                    String host = DOMAINS_MATCHER.group(1);
                    return new DNSRule(host);
                }
                return null;
            }
        }
    }

    private interface LineParser {
        public DNSRule parseLine(String line);
    }

    private static class DNSRule {
        String host, target;
        boolean ipv6, both = false;

        public DNSRule(String host){
            this.host = host;
            both = true;
        }

        public DNSRule(String host, String target, boolean IPv6) {
            this.host = host;
            this.target = target;
            this.ipv6 = IPv6;
        }
    }
}
