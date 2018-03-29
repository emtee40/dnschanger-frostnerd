package com.frostnerd.dnschanger.services.jobs;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.frostnerd.dnschanger.LogFactory;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ConnectivityJobAPI21 extends JobService{
    private static final String LOG_TAG = "[ConnectivityJobAPI21]";
    private NetworkCheckHandle handle;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        LogFactory.writeMessage(this, LOG_TAG, "Job created");
        handle = new NetworkCheckHandle(getApplicationContext() == null ? this : getApplicationContext(), LOG_TAG);
        LogFactory.writeMessage(this, LOG_TAG, "Done with onStartJob");
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        LogFactory.writeMessage(this, LOG_TAG, "Job stopped");
        handle.stop();
        return true;
    }
}
