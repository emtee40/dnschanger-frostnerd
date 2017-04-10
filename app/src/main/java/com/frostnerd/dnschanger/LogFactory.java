package com.frostnerd.dnschanger;

import android.content.Context;
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

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class LogFactory {
    private static File logFile;
    private static BufferedWriter fileWriter = null;
    private static boolean ready = false, usable = false;
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("kk_mm_ss_dd_MM_yyyy"),
            TIMESTAMP_FORMATTER = new SimpleDateFormat("EEE dd kk:mm:ss");

    public static boolean prepare(Context context) {
        if (ready) return usable;
        String name = "logFile_" + DATE_TIME_FORMATTER.format(new Date()) + ".log";
        logFile = new File(context.getFilesDir(), name);
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

    public static void writeMessage(Context context, String  customTag, String message) {
        if (prepare(context)) {
            try {
                fileWriter.write(TIMESTAMP_FORMATTER.format(new Date()) + " " + customTag + ": " + message);
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeMessage(Context context, Tag tag, String message) {
        if (prepare(context)) {
            try {
                fileWriter.write(TIMESTAMP_FORMATTER.format(new Date()) + (tag != null && tag != Tag.NO_TAG ? " " + tag : "") + ": " + message);
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeStackTrace(Context context, Tag tag, Throwable exception) {
        if (prepare(context)) {
            writeMessage(context, tag, stacktraceToString(exception));
        }
    }

    private static String stacktraceToString(Throwable throwable){
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
