package com.frostnerd.dnschanger.services.jobs;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.Util;

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
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectivityJobAPI21 extends JobService {
    private static final String LOG_TAG = "[ConnectivityJobAPI21]";
    private NetworkCheckHandle handle;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        LogFactory.writeMessage(this, LOG_TAG, "Job created");
        boolean initial = jobParameters == null || !jobParameters.getExtras().containsKey("initial") || (boolean) jobParameters.getExtras().get("initial");
        handle = Util.maybeCreateNetworkCheckHandle(this, LOG_TAG, initial);
        if (handle == null) {
            LogFactory.writeMessage(this, LOG_TAG, "Not starting handle because the respective settings aren't enabled");
            stopSelf();
            return false;
        }
        LogFactory.writeMessage(this, LOG_TAG, "Done with onStartJob");
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        LogFactory.writeMessage(this, LOG_TAG, "Job stopped");
        if (handle != null) handle.stop();
        return true;
    }
}
