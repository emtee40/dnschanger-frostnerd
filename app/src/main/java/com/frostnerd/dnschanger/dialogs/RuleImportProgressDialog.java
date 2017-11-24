package com.frostnerd.dnschanger.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.services.RuleImportService;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
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

    public RuleImportProgressDialog(@NonNull Activity context, List<ImportableFile> files, int databaseConflictHandling) {
        super(context, ThemeHandler.getDialogTheme(context));
        for(ImportableFile file: files)linesCombined += file.getLines();
        setTitle(getContext().getString(R.string.importing_x_rules).replace("[x]", "" + linesCombined));
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setButton(BUTTON_NEUTRAL, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //asyncImport.cancel(false);
                //asyncImport = null;
                dialog.dismiss();
            }
        });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //if(asyncImport != null)asyncImport.cancel(false);
                //asyncImport = null;
            }
        });
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_rule_import_progress, null, false));
        context.startService(RuleImportService.createIntent(context, linesCombined, databaseConflictHandling,
                files.toArray(new ImportableFile[files.size()])));
    }

    @Override
    public void dismiss() {
        super.dismiss();
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

    public enum FileType implements LineParser, Serializable {
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

    public interface LineParser {
        TemporaryDNSRule parseLine(String line);
    }

    public static final class TemporaryDNSRule {
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

        public String getHost() {
            return host;
        }

        public String getTarget() {
            return target;
        }

        public boolean isBoth() {
            return both;
        }

        public boolean isIpv6() {
            return ipv6;
        }
    }

    public static class ImportableFile implements Serializable{
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
}
