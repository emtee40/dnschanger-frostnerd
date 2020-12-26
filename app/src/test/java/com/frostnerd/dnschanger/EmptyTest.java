package com.frostnerd.dnschanger;

import com.frostnerd.logging.DetailedTimingLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.assertTrue;

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
