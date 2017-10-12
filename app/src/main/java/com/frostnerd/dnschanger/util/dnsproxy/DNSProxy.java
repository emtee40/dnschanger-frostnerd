package com.frostnerd.dnschanger.util.dnsproxy;

import android.system.ErrnoException;

import java.io.IOException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public abstract class DNSProxy {

    public abstract void run() throws InterruptedException, IOException, ErrnoException;
    public abstract void stop();
}
