package com.frostnerd.dnschanger;

import android.app.Application;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.activities.ErrorDialogActivity;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class DNSChanger extends Application {
    private static final String LOG_TAG = "[DNSCHANGER-APPLICATION]";
    private Thread.UncaughtExceptionHandler customHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LogFactory.writeMessage(DNSChanger.this,  new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, "Caught uncaught exception");
            LogFactory.writeStackTrace(DNSChanger.this, new String[]{LOG_TAG, LogFactory.Tag.ERROR.toString()}, e);
            ErrorDialogActivity.show(DNSChanger.this, e);
            System.exit(2);
        }
    };

    @Override
    public void onCreate() {
        setTheme(ThemeHandler.getAppTheme(this));
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
        API.terminate();
        LogFactory.terminate();
        super.onTerminate();
    }

    public Thread.UncaughtExceptionHandler getExcpetionHandler() {
        return customHandler;
    }
}
