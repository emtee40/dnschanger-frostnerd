package com.frostnerd.dnschanger;

import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

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
    private static boolean ready = false, usable = false;
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("kk_mm_ss_dd_MM_yyyy"),
            TIMESTAMP_FORMATTER = new SimpleDateFormat("EEE dd kk:mm:ss");

    public static boolean prepare(Context context) {
        if (ready) return usable;
        String name = "logFile_" + DATE_TIME_FORMATTER.format(new Date()) + ".log";
        logDir = new File(context.getFilesDir(), "logs/");
        logDir.mkdirs();
        logFile = new File(logDir, name);
        try {
            if (!logFile.canWrite() || !logFile.createNewFile()) {
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
        writeMessage(context, Tag.INFO, "Android Version: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")");
        writeMessage(context, Tag.INFO, "Device: " + Build.MODEL + " from " + Build.MANUFACTURER + " (Device: " + Build.DEVICE + ", Product: " + Build.PRODUCT + ")");
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
                builder.append(TIMESTAMP_FORMATTER.format(new Date()));
                for(String s: tags)builder.append(" " + s);
                builder.append(": " + message);
                if(printIntent)builder.append(" " + describeIntent(intent, true));
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
        builder.append("Intent{Action:" + intent.getAction() + ";Type" + intent.getType() + ";Package" + intent.getPackage() +
                ";Scheme:" + intent.getScheme() + ";Data:" + intent.getDataString() + ";ExtrasCount:" + intent.getExtras().size());
        if(printExtras){
            builder.append(";Extras:{");
            String key;
            for(Iterator<String> keys = intent.getExtras().keySet().iterator(); keys.hasNext();){
                key = keys.next();
                builder.append(key + "->" + intent.getExtras().get(key));
                if(keys.hasNext())builder.append(";");
            }
            builder.append("}");
        }
        builder.append("}");
        return builder.toString();
    }

    private static String stacktraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String res = sw.toString();
        try {
            sw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static enum Tag {
        INFO, ERROR, MESSAGE, NO_TAG
    }
}
