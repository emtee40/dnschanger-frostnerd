package com.frostnerd.dnschanger;

import android.app.Application;
import android.database.sqlite.SQLiteException;

import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.util.ThemeHandler;

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
            }
            if (defaultHandler != null) defaultHandler.uncaughtException(t, e);
        }
    };
    private Thread.UncaughtExceptionHandler defaultHandler;

    private boolean showErrorDialog(Throwable exception) {
        if(exception instanceof SQLiteException || (exception.getCause() != null && exception instanceof SQLiteException))return true;
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

    public Thread.UncaughtExceptionHandler getExceptionHandler() {
        return customHandler;
    }
}
