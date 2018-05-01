package com.frostnerd.dnschanger.util;

import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.logging.DetailedTimingLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;
/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
//@RunWith(RobolectricTestRunner.class)
public class UtilTest {
    private static final DetailedTimingLogger timingLogger = new DetailedTimingLogger("com.frostnerd.dnschanger", "UtilTest", false);
    @Rule
    public TestName testName = new TestName();
    private static final Set<String> IPV4_NoPort_Valid = new HashSet<>();
    private static final Set<String> IPV4_NoPort_Invalid = new HashSet<>();
    private static final Set<String> IPV6_NoPort_Valid = new HashSet<>();
    private static final Set<String> IPV6_NoPort_InValid = new HashSet<>();

    private static final Set<String> IPV4_Port_Valid = new HashSet<>();
    private static final Set<String> IPV4_Port_Invalid = new HashSet<>();
    private static final Set<String> IPV6_Port_Valid = new HashSet<>();
    private static final Set<String> IPV6_Port_InValid = new HashSet<>();
    private static final int PORT_USED = 99, INVALID_PORT_USED = 99999;

    @BeforeClass
    public static void setupClass() {
        timingLogger.reset();
        timingLogger.addSplit("Class setup");
        IPV4_NoPort_Valid.add("192.168.17.1");
        IPV4_NoPort_Valid.add("192.168.17.254");
        IPV4_NoPort_Valid.add("244.244.244.254");
        IPV4_NoPort_Valid.add("255.255.255.254");
        IPV4_NoPort_Valid.add("1.1.1.1");
        IPV4_NoPort_Valid.add("192.0.0.1");

        IPV4_NoPort_Invalid.add("0.0.0.0");
        IPV4_NoPort_Invalid.add("266.10.10.10");
        IPV4_NoPort_Invalid.add("10.10.10.266");

        IPV6_NoPort_Valid.add("::ffff:d043:de7b");
        IPV6_NoPort_Valid.add("2001:cdba:0000:0000:0000:0000:3257:9652");
        IPV6_NoPort_Valid.add("2001:cdba:0:0:0:0:3257:9652");
        IPV6_NoPort_Valid.add("2001:cdba::3257:9652");
        IPV6_NoPort_Valid.add("2001:3452:4952:2837::");
        IPV6_NoPort_Valid.add("2003:dead:beef:4dad:23:46:bb:101");

        IPV6_NoPort_InValid.add("GGGG:cdba::3257:9652");
        IPV6_NoPort_InValid.add("String");
        IPV6_NoPort_InValid.add("2001:cdba::GGGG:9652");
        IPV6_NoPort_InValid.add("::");

        for(DNSEntry entry: DNSEntry.defaultDNSEntries.keySet()){
            if(entry.getDns1() != IPPortPair.INVALID){
                IPV4_NoPort_Valid.add(entry.getDns1().toString(false));
            }
            if(entry.getDns2() != IPPortPair.INVALID){
                IPV4_NoPort_Valid.add(entry.getDns2().toString(false));
            }
            if(entry.getDns1V6() != IPPortPair.INVALID){
                IPV6_NoPort_Valid.add(entry.getDns1V6().toString(false));
            }
            if(entry.getDns2V6() != IPPortPair.INVALID){
                IPV6_NoPort_Valid.add(entry.getDns2V6().toString(false));
            }
        }

        for(String s: IPV4_NoPort_Valid){
            if(s.equals(""))continue;
            IPV4_Port_Valid.add(s + ":" + PORT_USED);
            IPV4_Port_Invalid.add(s + ":" + INVALID_PORT_USED);
        }

        for(String s: IPV4_NoPort_Invalid){
            if(s.equals(""))continue;
            IPV4_Port_Invalid.add(s + ":" + PORT_USED);
            IPV4_Port_Invalid.add(s + ":" + INVALID_PORT_USED);
        }

        for(String s: IPV6_NoPort_Valid){
            if(s.equals(""))continue;
            IPV6_Port_Valid.add("[" + s + "]:" + PORT_USED);
            IPV6_Port_InValid.add("[" + s + "]:" + INVALID_PORT_USED);
        }

        for(String s: IPV6_NoPort_InValid){
            if(s.equals(""))continue;
            IPV6_Port_InValid.add("[" + s + "]:" + PORT_USED);
            IPV6_Port_InValid.add("[" + s + "]:" + INVALID_PORT_USED);
        }

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
    public void testIPValidationNoPort() {
        assertTrue("Add some tests to this class.", true);
        IPPortPair result;
        for(String s: IPV4_NoPort_Valid){
            result = Util.validateInput(s, false, true, PORT_USED);
            assertTrue("IP '" + s + "' should be valid, but it isn't",
                    result != IPPortPair.INVALID && result != null &&
                            (result.getPort() == PORT_USED || result.getPort() == IPPortPair.getEmptyPair().getPort()));
        }
        for(String s: IPV6_NoPort_Valid){
            result = Util.validateInput(s, true, true, PORT_USED);
            assertTrue("IP '" + s + "' should be valid, but it isn't",
                    result != IPPortPair.INVALID && result != null &&
                            (result.getPort() == PORT_USED || result.getPort() == IPPortPair.getEmptyPair().getPort()));
        }

        for(String s: IPV4_NoPort_Invalid){
            result = Util.validateInput(s, false, true, PORT_USED);
            assertTrue("IP '" + s + "' should be invalid, but it isn't",
                    result == IPPortPair.INVALID || result == null);
        }
        for(String s: IPV6_NoPort_InValid){
            result = Util.validateInput(s, true, true, PORT_USED);
            assertTrue("IP '" + s + "' should be invalid, but it isn't",
                    result == IPPortPair.INVALID || result == null);
        }
    }

    @Test
    public void testIPValidationWithPort() {
        assertTrue("Add some tests to this class.", true);
        IPPortPair result;
        for(String s: IPV4_Port_Valid){
            result = Util.validateInput(s, false, true, -1);
            assertTrue("IP '" + s + "' should be valid, but it isn't",
                    result != IPPortPair.INVALID && result != null && result.getPort() == PORT_USED);
        }
        for(String s: IPV6_Port_Valid){
            result = Util.validateInput(s, true, true, -1);
            assertTrue("IP '" + s + "' should be valid, but it isn't",
                    result != IPPortPair.INVALID && result != null && result.getPort() == PORT_USED);
        }

        for(String s: IPV4_Port_Invalid){
            result = Util.validateInput(s, false, true, 99);
            assertTrue("IP '" + s + "' should be invalid, but it isn't",
                    result == IPPortPair.INVALID || result == null);
        }
        for(String s: IPV6_Port_InValid){
            result = Util.validateInput(s, true, true, 99);
            assertTrue("IP '" + s + "' should be invalid, but it isn't",
                    result == IPPortPair.INVALID || result == null);
        }
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