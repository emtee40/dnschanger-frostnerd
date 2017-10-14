package com.frostnerd.dnschanger.dialogs;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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
    private static final Pattern HOSTS_PATTERN = Pattern.compile("^(?:([^#\\s]+)\\s(((?:[0-9.[^#\\s]])+$)|(?:[0-9a-fA-F:[^#\\s]]+)))|(?:^(?:([0-9.]+)|([0-9a-fA-F:]+))\\s([^#\\s]+))");
    private static final Matcher HOSTS_MATCHER = HOSTS_PATTERN.matcher("");
    private static final Pattern DOMAINS_PATTERN = Pattern.compile("^([A-Za-z0-9]{1}[A-Za-z0-9\\-.]+)");
    private static final Matcher DOMAINS_MATCHER = DOMAINS_PATTERN.matcher("");
    private static final Pattern ADBLOCK_PATTERN = Pattern.compile("^\\|\\|([A-Za-z0-9]{1}[A-Za-z0-9\\-.]+)\\^");
    private static final Matcher ADBLOCK_MATCHER = ADBLOCK_PATTERN.matcher("");
    private int linesCombined;
    private Activity context;
    private TextView progressText, fileText;
    private List<ImportableFile> files;
    private AsyncTask<Void, Integer, Void> asyncImport = new AsyncTask<Void, Integer, Void>() {
        private int validLines = 0;

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
            SQLiteDatabase database = API.getDBHelper(getContext()).getWritableDatabase();
            database.beginTransaction();
            String line;
            DNSRule rule;
            ContentValues values = new ContentValues(3);
            int i = 0, pos = 0;
            for(ImportableFile file: files){
                BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
                LineParser parser = file.getFileType();
                onProgressUpdate(-1, pos++);
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
                reader.close();
            }
            if (!isCancelled()) database.setTransactionSuccessful();
            database.endTransaction();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext())).setTitle(R.string.done).setCancelable(true).
                    setNeutralButton(R.string.close, null).
                    setMessage(getContext().getString(R.string.rules_import_finished).replace("[x]", "" + linesCombined).replace("[y]", "" + validLines)).show();
            dismiss();
            if(context instanceof MainActivity){
                Fragment fragment = ((MainActivity)context).currentFragment();
                if(fragment instanceof RulesFragment){
                    ((RulesFragment)fragment).getRuleAdapter().reloadData();
                }
            }
        }

        @Override
        protected void onProgressUpdate(final Integer... values) {
            if(values.length == 2){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        fileText.setText(files.get(values[1]).getFile().getName());
                    }
                });
            }
            else{
                int i = values[0];
                progressText.setText(i + "/" + linesCombined);
            }
        }
    };

    public RuleImportProgressDialog(@NonNull Activity context, List<ImportableFile> files) {
        super(context, ThemeHandler.getDialogTheme(context));
        this.context = context;
        this.files = files;
        for(ImportableFile file: files)linesCombined += file.getLines();
        setTitle(getContext().getString(R.string.importing_x_rules).replace("[x]", "" + linesCombined));
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
        fileText = content.findViewById(R.id.file_name);
        asyncImport.execute();
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
            HashMap<FileType, Integer> validLines = new HashMap<>();
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
                    if(NetworkUtil.isIPv4(host) || (ipv6 = NetworkUtil.isIP(host, true))){
                        target = host;
                        host = HOSTS_MATCHER.group(2);
                    }else{
                        target = HOSTS_MATCHER.group(2);
                        ipv6 = NetworkUtil.isIP(target, true);
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
        }, ADBLOCK_FILE{
            @Override
            public DNSRule parseLine(String line) {
                if(ADBLOCK_MATCHER.reset(line).find()){
                    String host = ADBLOCK_MATCHER.group(1);
                    return new DNSRule(host);
                }
                return null;
            }
        };

        @Override
        public String toString() {
            return super.toString();
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

    public static class ImportableFile{
        private File file;
        private FileType fileType;
        private int lines;

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
}
