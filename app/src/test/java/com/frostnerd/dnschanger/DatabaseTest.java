package com.frostnerd.dnschanger;

import android.test.suitebuilder.annotation.LargeTest;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.utils.database.orm.Debug;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
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
@RunWith(RobolectricTestRunner.class)
@LargeTest
public class DatabaseTest {
    private DatabaseHelper helper;

    @Before
    public void setup(){
        helper = new DatabaseHelper(RuntimeEnvironment.application);
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
                new IPPortPair("8.8.4.4", 53, false), IPPortPair.EMPTY, null, "A new description", true);
        helper.insert(entry);
        assertNotNull("Inserted DNSEntry should be in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(entry)));
    }

    @Test
    public void testInsertIPPortPair(){
        IPPortPair pair = new IPPortPair("8.8.8.8", 53, false);
        helper.insert(pair);
        assertNotNull("Inserted IPPortPair should be in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(pair)));
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
        assertTrue("The host of the DNSRule inserted last should be 'blockeddomain.com'", helper.getLastRow(DNSRule.class).getHost().equals("blockeddomain.com"));
    }

    @Test
    public void testEditDNSRule(){
        helper.createDNSRule("blockeddomain.com", "0.0.0.0", false, false);
        assertTrue("The host of the DNSRule inserted last should be 'blockeddomain.com'", helper.getLastRow(DNSRule.class).getHost().equals("blockeddomain.com"));
        assertTrue("The target of the DNSRule inserted last should be '0.0.0.0'", helper.getLastRow(DNSRule.class).getTarget().equals("0.0.0.0"));
        helper.editDNSRule("blockeddomain.com", false, "192.168.178.1");
        assertTrue("The host of the DNSRule inserted last should be 'blockeddomain.com'", helper.getLastRow(DNSRule.class).getHost().equals("blockeddomain.com"));
        assertTrue("The target of the DNSRule inserted last should be '192.168.178.1'", helper.getLastRow(DNSRule.class).getTarget().equals("192.168.178.1"));
        helper.deleteDNSRule("blockeddomain.com", false);
        assertTrue("Table should now be empty", ParsedEntity.wrapEntity(DNSRule.class).getCount(helper) == 0);
    }

    @Test
    public void testInsertShortcut(){
        List<IPPortPair> pairs = Arrays.asList(new IPPortPair[]{new IPPortPair("8.8.8.8", 53, false), null,
                new IPPortPair("::1", 53, true), IPPortPair.EMPTY});
        helper.getSQLHandler(IPPortPair.class).insert(helper, pairs);
        Shortcut shortcut = new Shortcut("NewShortcut", pairs.get(0), pairs.get(1), pairs.get(2), pairs.get(3));
        helper.insert(shortcut);
        assertNotNull("Inserted Shortcut should be in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(shortcut)));
        helper.createShortcut("NewShortcut2", pairs.get(0), pairs.get(1), pairs.get(2), pairs.get(3));
        assertTrue("The name of the Shortcut inserted last should be 'NewShortcut2'", helper.getLastRow(Shortcut.class).getName().equals("NewShortcut2"));
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
        assertNotNull("Inserted DNSRule should be in the database", helper.select(DNSEntry.class, WhereCondition.buildBasedOnPrimaryKeys(dnsRuleImport)));
    }

    @Test
    public void testInsertDNSQuery(){
        DNSQuery query = new DNSQuery("8.8.8.8", false, System.currentTimeMillis());
        helper.insert(query);
        assertNotNull("Inserted DNSRule should be in the database",
                helper.select(DNSQuery.class, WhereCondition.buildBasedOnPrimaryKeys(query)));
    }
}
