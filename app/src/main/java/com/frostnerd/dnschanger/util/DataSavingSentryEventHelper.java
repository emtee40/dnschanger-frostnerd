package com.frostnerd.dnschanger.util;

import android.os.Build;

import com.frostnerd.dnschanger.BuildConfig;

import java.util.HashMap;
import java.util.Map;

import io.sentry.event.EventBuilder;
import io.sentry.event.helper.EventBuilderHelper;

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
public class DataSavingSentryEventHelper implements EventBuilderHelper {
    private Map<String, Map<String, Object>> contexts = new HashMap<>();

    public DataSavingSentryEventHelper() {
        Map<String, Object> osMap = new HashMap<>();
        Map<String, Object> appMap = new HashMap<>();

        osMap.put("name", "Android");
        osMap.put("version", Build.VERSION.RELEASE);
        osMap.put("build", Build.DISPLAY);
        appMap.put("app_version", BuildConfig.VERSION_NAME);
        appMap.put("app_identifier", "com.frostnerd.smokescreen");
        appMap.put("app_build", BuildConfig.VERSION_CODE);
        contexts.put("os", osMap);
        contexts.put("app", appMap);
    }

    @Override
    public void helpBuildingEvent(EventBuilder eventBuilder) {
        eventBuilder.withDist(BuildConfig.VERSION_CODE + "");
        eventBuilder.withRelease("com.frostnerd.dnschanger-" + BuildConfig.VERSION_NAME);
        eventBuilder.withContexts(contexts);
    }
}
