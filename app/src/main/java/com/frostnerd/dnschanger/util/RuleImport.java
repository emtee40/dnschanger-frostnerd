package com.frostnerd.dnschanger.util;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.frostnerd.dnschanger.services.RuleImportService;
import com.frostnerd.utils.networking.NetworkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
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
public class RuleImport {
    private static final Pattern DNSMASQ_PATTERN = Pattern.compile("^address=/([^/]+)/(?:([0-9.]+)|([0-9a-fA-F:]+))");
    private static final Matcher DNSMASQ_MATCHER = DNSMASQ_PATTERN.matcher(""),
            DNSMASQ_VALIDATION_MATCHER = DNSMASQ_PATTERN.matcher("");
    private static final Pattern HOSTS_PATTERN = Pattern.compile("^(?:([^#\\s]+)\\s+(((?:[0-9.[^#\\s]])+$)|(?:[0-9a-fA-F:[^#\\s]]+)))|(?:^(?:([0-9.]+)|([0-9a-fA-F:]+))\\s+([^#\\s]+))");
    private static final Matcher HOSTS_MATCHER = HOSTS_PATTERN.matcher(""),
            HOSTS_VALIDATION_MATCHER = HOSTS_PATTERN.matcher("");
    private static final Pattern DOMAINS_PATTERN = Pattern.compile("^([A-Za-z0-9][A-Za-z0-9\\-.]+)");
    private static final Matcher DOMAINS_MATCHER = DOMAINS_PATTERN.matcher(""),
            DOMAINS_VALIDATION_MATCHER = DOMAINS_PATTERN.matcher("");
    private static final Pattern ADBLOCK_PATTERN = Pattern.compile("^\\|\\|([A-Za-z0-9][A-Za-z0-9\\-.]+)\\^");
    private static final Matcher ADBLOCK_MATCHER = ADBLOCK_PATTERN.matcher(""),
            ADBLOCK_VALIDATION_MATCHER = ADBLOCK_PATTERN.matcher("");

    public static <T extends Activity &ImportStartedListener> void startImport(@NonNull T context, List<ImportableFile> files, int databaseConflictHandling){
        int linesCombined = 0;
        for(ImportableFile file: files)linesCombined += file.getLines();
        context.startService(RuleImportService.createIntent(context, linesCombined, databaseConflictHandling,
                files.toArray(new ImportableFile[files.size()])));
        context.importStarted(linesCombined);
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
            int lines = 0, fileLines = failFast ? 0 : getFileLines(f), validLinesBuffer;
            FileType won = null, focus = null;
            List<String> lineBuffer = null;
            while((line = reader.readLine()) != null && ((failFast && lines++ <= 300) || (!failFast && lines++ <= fileLines))){
                if(lines % 10000 == 0){ //We are in non-fail fast, try to focus on the best type yet
                    if(lineBuffer != null && lineBuffer.size() != 0){ //We had a focus before which wasn't successful in finding the Type
                        HashMap<FileType, Integer> validLinesTemp = new HashMap<>(validLines);
                        validLinesTemp.remove(focus); //Remove the current focus from the temporary map (Otherwise it would increase its line count twice)
                        for(Map.Entry<FileType, Integer> entry: validLinesTemp.entrySet()){ //Update the other types
                            if(entry.getKey().validateLine(line)){
                                validLinesTemp.put(entry.getKey(), validLinesBuffer=(entry.getValue()+1));
                                if(validLinesBuffer >= 50){
                                    won = entry.getKey();
                                    break;
                                }
                            }
                        }
                        validLines.putAll(validLinesTemp); //Add the result from the other types
                    }
                    Map.Entry<FileType, Integer> max = null;
                    for(Map.Entry<FileType, Integer> entry: validLines.entrySet()){ //Determine the type which had the most matches this far
                        if(max == null || entry.getValue().compareTo(max.getValue()) > 0)max = entry;
                    }
                    focus = max.getKey();
                    if(lineBuffer == null && lines+10000 <= fileLines)lineBuffer = new ArrayList<>(10001); //Create the linbuffer if there are 10000 lines left and it hasn't been created yet
                    else if(lineBuffer != null)lineBuffer.clear(); //There are more than 10000 lines left, clear the cache

                    if(lines+10000 <= fileLines)lineBuffer = null; //If there are less than 10000 lines left we do not need to cache them anymore
                }else if(focus != null){ //Use the determined focus
                    if(focus.validateLine(line)){
                        validLines.put(focus, validLinesBuffer=(validLines.get(focus)+1));
                        if(validLinesBuffer >= 50){
                            won = focus;
                            break;
                        }
                    }
                    // LineBuffer might be null because there is no sense in buffering anymore
                    if(lineBuffer != null)lineBuffer.add(line); //Safe the lines for the other types (will be queried if focus has no result)
                }else{ //Either we are in fail-fast or the focus isn't net
                    for(Map.Entry<FileType, Integer> entry: validLines.entrySet()){
                        if(entry.getKey().validateLine(line)){
                            validLines.put(entry.getKey(), validLinesBuffer=(entry.getValue()+1));
                            if(validLinesBuffer >= 50){
                                won = entry.getKey();
                                break;
                            }
                        }
                    }
                }
            }
            if(lineBuffer != null)lineBuffer.clear();
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

            @Override
            public boolean validateLine(String line) {
                if(DNSMASQ_VALIDATION_MATCHER.reset(line).find()){
                    String target = DNSMASQ_VALIDATION_MATCHER.group(2);
                    if(target != null && NetworkUtil.isIP(target, false)){
                        return true;
                    }else if((target = DNSMASQ_VALIDATION_MATCHER.group(3)) != null && NetworkUtil.isIP(target, true)){
                        return true;
                    }
                }
                return false;
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

            @Override
            public boolean validateLine(String line) {
                if(HOSTS_VALIDATION_MATCHER.reset(line).find()){
                    String host = HOSTS_VALIDATION_MATCHER.group(1), target;
                    boolean ipv6 = false;
                    if(NetworkUtil.isIPv4(host) || (ipv6 = NetworkUtil.isIP(host, true))){
                        target = host;
                    }else{
                        target = HOSTS_VALIDATION_MATCHER.group(2);
                        ipv6 = NetworkUtil.isIP(target, true);
                    }
                    return (!ipv6 && target.equals("0.0.0.0")) || NetworkUtil.isIP(target, ipv6);
                }
                return false;
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

            @Override
            public boolean validateLine(String line) {
                return ADBLOCK_VALIDATION_MATCHER.reset(line).find();
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

            @Override
            public boolean validateLine(String line) {
                return DOMAINS_VALIDATION_MATCHER.reset(line).find();
            }
        }

    }

    public interface LineParser {
        TemporaryDNSRule parseLine(String line);
        boolean validateLine(String line);
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

    public interface ImportStartedListener{
        void importStarted(int combinedLines);
    }
}
