package com.frostnerd.dnschanger.services.jobs;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
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
public class ConnectivityJobFirebase extends JobService {
    private static final String LOG_TAG = "[ConnectivityJobFireBase]";
    private NetworkCheckHandle handle;

    @Override
    public boolean onStartJob(JobParameters job) {
        LogFactory.writeMessage(this, LOG_TAG, "Job created");
        handle = new NetworkCheckHandle(this, LOG_TAG);
        LogFactory.writeMessage(this, LOG_TAG, "Done with onStartJob");
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters job) {
        LogFactory.writeMessage(this, LOG_TAG, "Job stopped");
        handle.stop();
        return true;
    }
}
