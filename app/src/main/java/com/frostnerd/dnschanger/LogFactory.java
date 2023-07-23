package com.frostnerd.dnschanger;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.PreferencesAccessor;

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

/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
public class LogFactory {
    private static File logFile;
    private static File logDir;
    private static BufferedWriter fileWriter = null;
    private static boolean ready = false, usable = false, enabled = false;
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("dd_MM_yyyy___kk_mm_ss", Locale.US),
            TIMESTAMP_FORMATTER = new SimpleDateFormat("EEE MMM dd.yy kk:mm:ss", Locale.US);
    public static final String STATIC_TAG = "[STATIC]";
    private static final boolean printMessagesToConsole = BuildConfig.DEBUG;

    public static synchronized File zipLogFiles(Context c){
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
            BufferedInputStream in;
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

    public synchronized static void enable(Context context){
        ready = false;
        enabled = true;
        if(fileWriter != null){
            try{
                fileWriter.close();
            }catch(Exception ignored){

            }
            fileWriter = null;
        }
        prepare(context);
    }

    public synchronized static void disable(){
        enabled = false;
        ready = true;
        usable = false;
        if(fileWriter != null){
            try{
                fileWriter.close();
            }catch(Exception ignored){

            }
            fileWriter = null;
        }
    }

    public synchronized static void terminate(){
        try{
            fileWriter.close();
        }catch(Exception ignored){

        }
        fileWriter = null;logFile=null;logDir = null;
        ready = usable = false;
    }

    public static void deleteLogFiles(Context context){
        if(logDir == null)logDir = new File(context.getFilesDir(), "logs/");
        boolean wasEnabled = ready && enabled;
        disable();
        for(File f: logDir.listFiles()){
            f.delete();
        }
        if(wasEnabled)enable(context);
    }

    private static synchronized boolean prepare(Context context) {
        if(!enabled && ready)return false;
        if (ready) return usable;
        if(context == null) return usable;
        enabled = PreferencesAccessor.isDebugEnabled(context);
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
        StringBuilder s = new StringBuilder();
        Map<String,Object> prefs = Preferences.getInstance(context).getAll(false);
        for(Map.Entry<String, Object> entry: prefs.entrySet())
            s.append(entry.getKey()).append("->").append(entry.getValue()).append("; ");
        writeMessage(context, Tag.INFO, "Preferences: " + s);
        writeMessage(context, Tag.INFO, "Prepare caller stack: " + stacktraceToString(new Throwable(), true));
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

    public static synchronized void writeMessage(Context context, String[] tags, String message, Intent intent, boolean printIntent){
        if (prepare(context)) {
            try {
                StringBuilder builder = new StringBuilder();
                builder.append("[");
                builder.append(System.currentTimeMillis());
                builder.append("] >");
                builder.append(TIMESTAMP_FORMATTER.format(new Date()));
                builder.append("<");
                for(String s: tags)if(!s.equalsIgnoreCase(Tag.NO_TAG.toString()))
                    builder.append(" ").append(s);
                builder.append(": ").append(message);
                if(printIntent) builder.append(" ").append(describeIntent(intent, true));
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
        writeSeparateStackTrace(context, exception);
    }

    public static void writeStackTrace(Context context, String[] tags, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tags, stacktraceToString(exception));
        }
        writeSeparateStackTrace(context, exception);
    }

    public static void writeStackTrace(Context context, Tag tag, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tag, stacktraceToString(exception));
        }
        writeSeparateStackTrace(context, exception);
    }

    public static void writeStackTrace(Context context, Tag[] tags, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tags, stacktraceToString(exception));
        }
        writeSeparateStackTrace(context, exception);
    }

    private static void writeSeparateStackTrace(Context context, Throwable exception){
        File dir = new File(context.getFilesDir(), "logs/");
        if(!dir.exists())dir.mkdirs();
        File f = new File(dir, DATE_TIME_FORMATTER.format(new Date()) + ".error.log");
        try {
            if(!f.exists())f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write("App Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n");
            writer.write("Android Version: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ", " +
                    "Incremental: " + Build.VERSION.INCREMENTAL + ")\n");
            writer.write(stacktraceToString(exception) + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        builder.append("Intent{Action:").append(intent.getAction()).append("; Type:").append(intent.getType())
                .append("; Package:").append(intent.getPackage()).append("; Scheme:").append(intent.getScheme())
                .append("; Data:").append(intent.getDataString()).append("; Component: ")
                .append(intent.getComponent() != null ? intent.getComponent().toShortString() : "Null;")
        .append("Categories: ").append(intent.getCategories()).append(";")
        .append("Flags: ").append(intent.getFlags()).append(";");
        if(intent.getExtras() != null)
            builder.append("ExtrasCount:").append(intent.getExtras().size());
        if(printExtras){
            builder.append("; Extras:{");
            if(intent.getExtras() == null)builder.append("Null}");
            else{
                String key;
                for(Iterator<String> keys = intent.getExtras().keySet().iterator(); keys.hasNext();){
                    key = keys.next();
                    builder.append(key).append("->").append(intent.getExtras().get(key));
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

    private synchronized static long getTotalMemory() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(    localFileReader, 8192);
            str2 = localBufferedReader.readLine();//meminfo
            arrayOfString = str2.split("\\s+");
            localBufferedReader.close();
            return Integer.parseInt(arrayOfString[1]) * 1024;
        }
        catch (IOException e){
            return -1;
        }
    }

    public enum Tag {
        INFO, ERROR, MESSAGE, NO_TAG
    }
}
