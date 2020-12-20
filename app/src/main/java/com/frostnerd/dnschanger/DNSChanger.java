package com.frostnerd.dnschanger;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteException;

import androidx.annotation.Keep;

import com.frostnerd.dnschanger.activities.ErrorDialogActivity;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.util.DataSavingSentryEventProcessor;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.ThemeHandler;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import io.sentry.Integration;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import io.sentry.android.core.*;
import io.sentry.protocol.User;

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
            maybeReportSentry(e);
            try {
                Thread.sleep(750);
            } catch (InterruptedException ignored) {}
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
    @Keep private DatabaseHelper helper;
    private Preferences mPreferences;
    private Boolean sentryInitialized = false, sentryInitializing = false, sentryDisabled = false;
    // Sometimes you just have to say f it. Don't ask me why, but Context is sometimes null in MainFragment
    public static Context context;

    private boolean showErrorDialog(Throwable exception) {
        if(exception instanceof SQLiteException || (exception.getCause() != null && exception instanceof SQLiteException))return true;
        return exception.getMessage() != null && exception.getMessage().toLowerCase().contains("cannot create interface");
    }

    @Override
    public void onCreate() {
        setTheme(ThemeHandler.getAppTheme(this));
        context = this;
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(customHandler);
        super.onCreate();
        LogFactory.writeMessage(this, LOG_TAG, "Application created");
        helper = DatabaseHelper.getInstance(this);
        mPreferences = Preferences.getInstance(this);
        setupSentry();
    }

    public void maybeReportSentry(Throwable ex) {
        if(sentryInitialized && !sentryDisabled) {
            Sentry.captureException(ex);
        }
    }

    // Creates the SentryClient
    // Absolutely no identifiable data is transmitted (Thus it is not subject to GDPR)
    // The Sentry instance does not store IP addresses
    // Absolutely no tracking is possible.
    // Can be turned off in the settings.
    public void setupSentry() {
        //noinspection ConstantConditions
        if(!sentryInitialized && !sentryInitializing && BuildConfig.SENTRY_ENABLED && !BuildConfig.SENTRY_DSN.equals("dummy")) {
            sentryInitializing = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean enabled = !mPreferences.getBoolean("disable_crash_reporting", false);
                        if(enabled) {
                            String hostname = InetAddress.getLocalHost().getHostName();
                            if(hostname.toLowerCase().contains("mars-sandbox")){
                                sentryDisabled = true;
                                return;
                            }
                            SentryAndroid.init(DNSChanger.this, new Sentry.OptionsConfiguration<SentryAndroidOptions>() {
                                @Override
                                public void configure(SentryAndroidOptions options) {
                                    options.setDsn(BuildConfig.SENTRY_DSN);
                                    setupSentryForDatasaving(options);
                                }
                            });
                            User user = new User();
                            user.setUsername("anon-" + BuildConfig.VERSION_CODE);
                            Sentry.setUser(user);
                            Sentry.setTag("dist", BuildConfig.VERSION_CODE + "");
                            Sentry.setTag(
                                    "app.installer_package",
                                    getPackageManager().getInstallerPackageName(getPackageName())
                            );
                            Sentry.setTag("richdata", "false");
                            sentryInitialized = true;
                        }
                    } catch (Throwable ignored) {

                    } finally {
                        sentryInitializing = false;
                    }
                }
            }).start();
        }
    }

    private void setupSentryForDatasaving(SentryAndroidOptions options) {
        List<Integration> toBeRemoved = new ArrayList<>();
        for (Integration integration : options.getIntegrations()) {
            if (integration instanceof PhoneStateBreadcrumbsIntegration ||
                    integration instanceof SystemEventsBreadcrumbsIntegration ||
                    integration instanceof TempSensorBreadcrumbsIntegration
            ) toBeRemoved.add(integration);
        }
        for (Integration integration : toBeRemoved) {
            options.getIntegrations().remove(integration);
        }
        options.addEventProcessor(new DataSavingSentryEventProcessor());
    }

    public void tearDownSentry() {
        Sentry.close();
        sentryInitialized = false;
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
