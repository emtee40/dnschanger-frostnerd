package com.frostnerd.dnschanger;

import com.frostnerd.utils.general.DetailedTimingLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertTrue;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
//@RunWith(RobolectricTestRunner.class)
public class EmptyTest {
    private static final DetailedTimingLogger timingLogger = new DetailedTimingLogger("com.frostnerd.dnschanger", "NAME", false); //TODO
    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setupClass() {
        timingLogger.reset();
        timingLogger.addSplit("Class setup");
    }

    @Before
    public void setup() {
        timingLogger.addSplit("Started Test setup for test " + testName.getMethodName());
        timingLogger.disableGlobally();
        /*
            Any test specific setup here
        */
        timingLogger.enableGlobally();
        timingLogger.addSplit("Starting test " + testName.getMethodName(), 1);
        timingLogger.disableGlobally();
    }

    @Test
    public void firstTest() {
        assertTrue("Add some tests to this class.", true);
    }

    @After
    public void cleanup() {
        timingLogger.enableGlobally();
        timingLogger.addSplit("Finished Test " + testName.getMethodName(), 1);
        timingLogger.disableGlobally();
        /*
            Any test specific teardown here
         */
        timingLogger.enableGlobally();
        timingLogger.addSplit("Finished cleanup for test " + testName.getMethodName());
    }

    @AfterClass
    public static void classCleanup() {
        timingLogger.enableGlobally();
        timingLogger.dumpToLog(10);
    }
}
