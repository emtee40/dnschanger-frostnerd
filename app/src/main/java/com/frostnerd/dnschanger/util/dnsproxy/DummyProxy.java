package com.frostnerd.dnschanger.util.dnsproxy;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class DummyProxy extends DNSProxy {
    private boolean shouldRun = true;

    public DummyProxy(){
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CREATING DUMMY");
    }

    @Override
    public void run() throws InterruptedException {
        while(shouldRun){
            Thread.sleep(250);
        }
    }

    @Override
    public void stop() {
        shouldRun = false;
    }
}
