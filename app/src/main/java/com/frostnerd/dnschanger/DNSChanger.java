package com.frostnerd.dnschanger;

import android.app.Application;

import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class DNSChanger extends Application {
    private static final String LOG_TAG = "[DNSCHANGER-APPLICATION]";
    private final Thread.UncaughtExceptionHandler customHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LogFactory.writeMessage(DNSChanger.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, "Caught uncaught exception");
            LogFactory.writeStackTrace(DNSChanger.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, e);
            if (showErrorDialog(e)) {
                ErrorDialogActivity.show(DNSChanger.this, e);
                System.exit(2);
            } else if (defaultHandler != null) defaultHandler.uncaughtException(t, e);
        }
    };
    private Thread.UncaughtExceptionHandler defaultHandler;

    private boolean showErrorDialog(Throwable exception) {
        return exception.getMessage() != null && exception.getMessage().toLowerCase().contains("cannot create interface");
    }

    @Override
    public void onCreate() {
        setTheme(ThemeHandler.getAppTheme(this));
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(customHandler);
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Application created");
    }

    @Override
    public void onLowMemory() {
        LogFactory.writeMessage(this, LOG_TAG, "Application got message about low memory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        LogFactory.writeMessage(this, LOG_TAG, "Memory was trimmed. Level: " + level);
        super.onTrimMemory(level);
    }

    @Override
    public void onTerminate() {
        LogFactory.writeMessage(this, LOG_TAG, "Application terminated");
        Util.getDBHelper(this).close();
        LogFactory.terminate();
        super.onTerminate();
    }

    public Thread.UncaughtExceptionHandler getExceptionHandler() {
        return customHandler;
    }
}
