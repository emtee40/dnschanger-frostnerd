package com.frostnerd.dnschanger;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.util.Log;

import com.frostnerd.utils.preferences.Preferences;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class LogFactory {
    private static File logFile;
    private static File logDir;
    private static BufferedWriter fileWriter = null;
    private static boolean ready = false, usable = false, enabled = false;
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("dd_MM_yyyy___kk_mm_ss", Locale.US),
            TIMESTAMP_FORMATTER = new SimpleDateFormat("EEE MMM dd.yy kk:mm:ss", Locale.US);
    public static final String STATIC_TAG = "[STATIC]";
    private static final boolean printMessagesToConsole = true;


    public static File zipLogFiles(Context c){
        if(logDir == null || !logDir.canWrite() || !logDir.canRead())return null;
        writeMessage(c, Tag.INFO, "Exporting Log files");
        try{
            File zipFile = new File(logDir, "logs.collection");
            if(zipFile.exists())zipFile.delete();
            File[] logFiles = logDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".log");
                }
            });
            BufferedInputStream in = null;
            FileOutputStream dest = new FileOutputStream(zipFile);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            byte[] buffer = new byte[2048];

            for(File f: logFiles){
                FileInputStream fi = new FileInputStream(f);
                in = new BufferedInputStream(fi, buffer.length);
                ZipEntry entry = new ZipEntry(f.getName().substring(f.getName().lastIndexOf("/")+1));
                out.putNextEntry(entry);
                int count;
                while ((count = in.read(buffer, 0, buffer.length)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.flush();
                in.close();
            }
            out.close();
            dest.close();
            writeMessage(c, Tag.INFO, "Log files exported into zip file");
            return zipFile;
        }catch(Exception e){
            e.printStackTrace();
            writeStackTrace(c, Tag.ERROR, e);
        }
        return null;
    }

    public static void enable(){
        enabled = true;
        ready = false;
        usable = false;
        if(fileWriter != null){
            try{
                fileWriter.close();
            }catch(Exception e){

            }
            fileWriter = null;
        }
    }

    public static void disable(){
        enabled = false;
        ready = true;
        usable = false;
        if(fileWriter != null){
            try{
                fileWriter.close();
            }catch(Exception e){

            }
            fileWriter = null;
        }
    }

    public static void terminate(){
        try{
            fileWriter.close();
        }catch(Exception e){

        }
        fileWriter = null;logFile=null;logDir = null;
        ready = usable = false;
    }

    public static boolean prepare(Context context) {
        if(!enabled && ready)return false;
        if (ready) return usable;
        enabled = Preferences.getBoolean(context, "debug", false);
        if(!enabled){
            ready = true;
            enabled = false;
            usable = false;
            return false;
        }
        String name = "logFile_" + DATE_TIME_FORMATTER.format(new Date()) + ".log";
        logDir = new File(context.getFilesDir(), "logs/");
        logDir.mkdirs();
        logFile = new File(logDir, name);
        try {
            if (!logDir.canWrite() || !logFile.createNewFile() || !logFile.canWrite()) {
                ready = true;
                return usable = false;
            }
            fileWriter = new BufferedWriter(new FileWriter(logFile));
            ready = usable = true;
        } catch (IOException e) {
            ready = true;
            usable = false;
            e.printStackTrace();
        }
        writeMessage(context, Tag.INFO, "App Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        writeMessage(context, Tag.INFO, "Android Version: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ", " +
                "Incremental: " + Build.VERSION.INCREMENTAL + ")");
        writeMessage(context, Tag.INFO, "Device: " + Build.MODEL + " from " + Build.MANUFACTURER + " (Device: " + Build.DEVICE + ", Product: " + Build.PRODUCT + ")");
        writeMessage(context, Tag.INFO, "Language: " + Locale.getDefault().getDisplayLanguage());
        writeMessage(context, Tag.INFO, "Device RAM: " + getTotalMemory());
        String s = "";
        Map<String,Object> prefs = Preferences.getAll(context, false);
        for(String key: prefs.keySet())s += key + "->" + prefs.get(key) + "; ";
        writeMessage(context, Tag.INFO, "Preferences: " + s);
        writeMessage(context, Tag.NO_TAG, "--------------------------------------------------");
        return usable;
    }

    public static void writeMessage(Context context, String customTag, String message,Intent intent) {
        writeMessage(context, customTag, message, intent, true);
    }

    public static void writeMessage(Context context, String[] customTags, String message,Intent intent) {
        writeMessage(context, customTags, message, intent, true);
    }

    private static void writeMessage(Context context, String customTag, String message,Intent intent, boolean printIntent) {
        writeMessage(context, new String[]{customTag}, message, intent, printIntent);
    }

    private static void writeMessage(Context context, Tag[] tags, String message,Intent intent, boolean printIntent) {
        String[] arr = new String[tags.length];
        for(int i = 0; i < tags.length;i++)arr[i] = tags[i].toString();
        writeMessage(context, arr, message, intent, printIntent);
    }

    public static void writeMessage(Context context, String[] tags, String message, Intent intent, boolean printIntent){
        if (prepare(context)) {
            try {
                StringBuilder builder = new StringBuilder();
                builder.append("[");
                builder.append(System.currentTimeMillis());
                builder.append("] >");
                builder.append(TIMESTAMP_FORMATTER.format(new Date()));
                builder.append("<");
                for(String s: tags)if(!s.equalsIgnoreCase(Tag.NO_TAG.toString()))builder.append(" " + s);
                builder.append(": " + message);
                if(printIntent)builder.append(" " + describeIntent(intent, true));
                if(printMessagesToConsole) System.out.println(builder.toString().split("<")[1]);
                builder.append("\n");
                fileWriter.write(builder.toString());
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMessage(Context context, Tag tag, String message) {
        writeMessage(context, tag.toString(), message, null, false);
    }

    public static void writeMessage(Context context, Tag[] tags, String message) {
        writeMessage(context, tags, message, null, false);
    }

    public static void writeMessage(Context context, Tag tag, String message, Intent intent) {
        writeMessage(context, tag.toString(), message, intent, true);
    }

    public static void writeMessage(Context context, Tag[] tags, String message, Intent intent) {
        writeMessage(context, tags, message, intent, true);
    }

    public static void writeMessage(Context context, String customTag, String message) {
        writeMessage(context, customTag, message, null, false);
    }

    public static void writeMessage(Context context, String[] customTags, String message) {
        writeMessage(context, customTags, message, null, false);
    }

    public static void writeStackTrace(Context context, String customTag, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, customTag, stacktraceToString(exception));
        }
    }

    public static void writeStackTrace(Context context, String[] tags, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tags, stacktraceToString(exception));
        }
    }

    public static void writeStackTrace(Context context, Tag tag, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tag, stacktraceToString(exception));
        }
    }

    public static void writeStackTrace(Context context, Tag[] tags, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tags, stacktraceToString(exception));
        }
    }

    public static void writeCurrentStack(Context context, String customTag) {
        writeStackTrace(context, customTag, new Throwable());
    }

    public static void writeCurrentStack(Context context, Tag tag) {
        writeStackTrace(context, tag, new Throwable());
    }

    public static void writeCurrentStack(Context context, String[] customTags) {
        writeStackTrace(context, customTags, new Throwable());
    }

    public static void writeCurrentStack(Context context, Tag[] tags) {
        writeStackTrace(context, tags, new Throwable());
    }

    public static String describeIntent(Intent intent, boolean printExtras){
        if(intent == null)return "Intent{NullIntent}";
        StringBuilder builder = new StringBuilder();
        builder.append("Intent{Action:" + intent.getAction() + "; Type:" + intent.getType() + "; Package:" + intent.getPackage() +
                "; Scheme:" + intent.getScheme() + "; Data:" + intent.getDataString() + ";");
        if(intent.getExtras() != null)builder.append("ExtrasCount:" + intent.getExtras().size());
        if(printExtras){
            builder.append("; Extras:{");
            if(intent.getExtras() == null)builder.append("Null}");
            else{
                String key;
                for(Iterator<String> keys = intent.getExtras().keySet().iterator(); keys.hasNext();){
                    key = keys.next();
                    builder.append(key + "->" + intent.getExtras().get(key));
                    if(keys.hasNext())builder.append("; ");
                }
                builder.append("}");
            }
        }
        builder.append("}");
        return builder.toString();
    }

    public static String stacktraceToString(Throwable throwable) {
        return stacktraceToString(throwable,false);
    }

    public static String stacktraceToString(Throwable throwable, boolean replaceNewline) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String res = sw.toString();
        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return replaceNewline ? res.replace("\n", " -- ") : res;
    }

    private static long getTotalMemory() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(    localFileReader, 8192);
            str2 = localBufferedReader.readLine();//meminfo
            arrayOfString = str2.split("\\s+");
            localBufferedReader.close();
            return Integer.valueOf(arrayOfString[1]).intValue() * 1024;
        }
        catch (IOException e){
            return -1;
        }
    }

    public enum Tag {
        INFO, ERROR, MESSAGE, NO_TAG
    }
}
