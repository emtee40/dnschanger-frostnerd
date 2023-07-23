package com.frostnerd.dnschanger.util;

import android.os.Build;

import com.frostnerd.dnschanger.BuildConfig;
import io.sentry.EventProcessor;
import io.sentry.SentryEvent;
import io.sentry.protocol.App;
import io.sentry.protocol.Device;
import io.sentry.protocol.OperatingSystem;




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


public class DataSavingSentryEventProcessor implements EventProcessor {

    // Override default values capture by Sentry to remove statistical data
    // You're welcome!
    @Override
    public SentryEvent process(SentryEvent event, Object hint) {
        event.getContexts().setDevice(new Device()); // Normally this would contain all sorts of data about the device I'd never need or use. Overriding removes them.

        App app = new App();
        app.setAppVersion(BuildConfig.VERSION_NAME);
        app.setAppVersion("com.frostnerd.dnschanger");
        app.setAppName("DNSChanger");
        app.setAppBuild(BuildConfig.VERSION_CODE + "");

        OperatingSystem os = new OperatingSystem();
        os.setName("Android");
        os.setVersion(Build.VERSION.RELEASE);
        os.setBuild(Build.DISPLAY);

        event.setDist(BuildConfig.VERSION_CODE + "");
        return event;
    }
}
