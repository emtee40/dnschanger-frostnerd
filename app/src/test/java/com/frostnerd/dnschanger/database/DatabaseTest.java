package com.frostnerd.dnschanger.database;


import com.frostnerd.database.orm.parser.ParsedEntity;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.logging.DetailedTimingLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
@RunWith(RobolectricTestRunner.class)
public class DatabaseTest {
    @Rule
    public TestName testName = new TestName();
    private static final DetailedTimingLogger timingLogger = new DetailedTimingLogger("com.frostnerd.dnschanger", "DatabaseTest", false);
    private DatabaseHelper helper;

    @BeforeClass
    public static void setupClass() {
        timingLogger.reset();
        timingLogger.addSplit("Class setup");
    }

    @Before
    public void setup() {
        timingLogger.addSplit("Started Test setup for test " + testName.getMethodName());
        timingLogger.disableGlobally();
        helper = DatabaseHelper.getInstance(RuntimeEnvironment.application);
        helper.getWritableDatabase();
        /*
            Any test specific setup here
        */
        timingLogger.enableGlobally();
        timingLogger.addSplit("Starting test " + testName.getMethodName(), 1);
        timingLogger.disableGlobally();
    }

    @Test
    public void testDefaultDNSAddressesInserted(){
        for(DNSEntry entry: DNSEntry.defaultDNSEntries.keySet()){
            assertNotNull("The default DNS entries should exist in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(entry)));
        }
    }

    @Test
    public void testInsertDNSEntry(){
        DNSEntry entry = new DNSEntry("MyCoolName", "Short", new IPPortPair("8.8.8.8", 53, false),
                new IPPortPair("8.8.4.4", 53, false), IPPortPair.getEmptyPair(), null, "A new description", true);
        helper.insert(entry);
        assertNotNull("Inserted DNSEntry should be in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(entry)));
    }

    @Test
    public void testInsertIPPortPair(){
        IPPortPair pair = new IPPortPair("8.8.8.8", 53, false);
        helper.insert(pair);
        assertNotNull("Inserted IPPortPair should be in the database",
                helper.select(IPPortPair.class, WhereCondition.buildBasedOnPrimaryKeys(pair)));
    }

    @Test
    public void testInsertDNSRules(){
        List<DNSRule> inserted = new ArrayList<>();
        for(int i = 1; i <= 10; i++){
            inserted.add(new DNSRule(i + ".frostnerd.com", "192.168.178." + i, false, false));
            helper.insert(inserted.get(inserted.size()-1));
            assertTrue("The method dnsRuleExists of the DatabaseHelper should return true", helper.dnsRuleExists(inserted.get(inserted.size() -1 ).getHost(), false));
        }
        for(DNSRule fromDB: ParsedEntity.wrapEntity(DNSRule.class).getAll(helper)){
            assertTrue("Because DNSRule is a Multiton the existing instances should be returned", inserted.contains(fromDB));
        }
        DNSRule rule = new DNSRule("frostnerd.com", "127.0.0.1", false, false);
        helper.insert(rule);
        assertNotNull("Inserted DNSRule should be in the database",
                helper.select(DNSRule.class, WhereCondition.buildBasedOnPrimaryKeys(rule)));
        helper.createDNSRule("blockeddomain.com", "0.0.0.0", false, false);
        assertEquals("The host of the DNSRule inserted last should be 'blockeddomain.com'", "blockeddomain.com", helper.getLastRow(DNSRule.class).getHost());
    }

    @Test
    public void testEditDNSRule(){
        helper.createDNSRule("blockeddomain.com", "0.0.0.0", false, false);
        assertEquals("The host of the DNSRule inserted last should be 'blockeddomain.com'", "blockeddomain.com", helper.getLastRow(DNSRule.class).getHost());
        assertEquals("The target of the DNSRule inserted last should be '0.0.0.0'", "0.0.0.0", helper.getLastRow(DNSRule.class).getTarget());
        helper.editDNSRule("blockeddomain.com", false, "192.168.178.1");
        assertEquals("The host of the DNSRule inserted last should be 'blockeddomain.com'", "blockeddomain.com", helper.getLastRow(DNSRule.class).getHost());
        assertEquals("The target of the DNSRule inserted last should be '192.168.178.1'", "192.168.178.1", helper.getLastRow(DNSRule.class).getTarget());
        helper.deleteDNSRule("blockeddomain.com", false);
        assertEquals("Table should now be empty", 0, ParsedEntity.wrapEntity(DNSRule.class).getCount(helper));
    }

    @Test
    public void testInsertShortcut(){
        List<IPPortPair> pairs = Arrays.asList(new IPPortPair("8.8.8.8", 53, false), null,
                new IPPortPair("::1", 53, true), IPPortPair.getEmptyPair());
        helper.getSQLHandler(IPPortPair.class).insert(helper, pairs);
        Shortcut shortcut = new Shortcut("NewShortcut", pairs.get(0), pairs.get(1), pairs.get(2), pairs.get(3));
        helper.insert(shortcut);
        assertNotNull("Inserted Shortcut should be in the database", helper.select(Shortcut.class, WhereCondition.buildBasedOnPrimaryKeys(shortcut)));
        helper.createShortcut("NewShortcut2", pairs.get(0), pairs.get(1), pairs.get(2), pairs.get(3));
        assertEquals("The name of the Shortcut inserted last should be 'NewShortcut2'", "NewShortcut2", helper.getLastRow(Shortcut.class).getName());
    }

    @Test
    public void testInsertDNSRuleImport(){
        List<DNSRule> inserted = new ArrayList<>();
        for(int i = 1; i <= 10; i++){
            inserted.add(new DNSRule(i + ".frostnerd.com", "192.168.178." + i, false, false));
            helper.insert(inserted.get(inserted.size()-1));
        }
        DNSRuleImport dnsRuleImport = new DNSRuleImport("MyFile.txt", System.currentTimeMillis(),
                inserted.get(0).getRowid(),
                inserted.get(inserted.size() -1).getRowid());
        helper.insert(dnsRuleImport);
        assertNotNull("Inserted DNSRuleImport should be in the database", helper.select(DNSRuleImport.class, WhereCondition.buildBasedOnPrimaryKeys(dnsRuleImport)));
    }

    @Test
    public void testInsertDNSQuery(){
        DNSQuery query = new DNSQuery("8.8.8.8", false, System.currentTimeMillis());
        helper.insert(query);
        assertNotNull("Inserted DNSRule should be in the database",
                helper.select(DNSQuery.class, WhereCondition.buildBasedOnPrimaryKeys(query)));
    }

    @After
    public void cleanup() {
        timingLogger.enableGlobally();
        timingLogger.addSplit("Finished Test " + testName.getMethodName(), 1);
        timingLogger.disableGlobally();
        /*
            Any test specific teardown here
         */
        helper.close();
        timingLogger.enableGlobally();
        timingLogger.addSplit("Finished cleanup for test " + testName.getMethodName());
    }

    @AfterClass
    public static void classCleanup() {
        timingLogger.enableGlobally();
        timingLogger.dumpToLog(10);
    }
}
