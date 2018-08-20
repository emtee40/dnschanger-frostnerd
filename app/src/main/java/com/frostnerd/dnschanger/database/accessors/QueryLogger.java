package com.frostnerd.dnschanger.database.accessors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.measite.minidns.DNSMessage;
import de.measite.minidns.Record;
import de.measite.minidns.record.Data;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class QueryLogger {
    private DatabaseHelper helper;
    private final String insertStatement;
    private static Runnable newQueryLogged;
    private boolean logUpstreamAnswers;
    private final List<WaitingQuery> waitingQueries = new LinkedList<>();

    public QueryLogger(DatabaseHelper databaseHelper, boolean logUpstreamAnswers){
        this.helper = databaseHelper;
        this.logUpstreamAnswers = logUpstreamAnswers;
        String host = databaseHelper.findColumnOrThrow(DNSQuery.class, "host").getColumnName(),
                ipv6 = databaseHelper.findColumnOrThrow(DNSQuery.class, "ipv6").getColumnName(),
                time = databaseHelper.findColumnOrThrow(DNSQuery.class, "time").getColumnName();
        insertStatement = "INSERT INTO " + databaseHelper.getTableName(DNSQuery.class) + "(" + host +
                "," + ipv6 + "," + time + ")VALUES(?,?,?)";
    }

    public void logQuery(DNSMessage dnsMessage, boolean ipv6){
        helper.getWritableDatabase().execSQL(insertStatement, new Object[]{dnsMessage.getQuestion().name, ipv6, System.currentTimeMillis()});
        if(newQueryLogged != null)newQueryLogged.run();
        if(logUpstreamAnswers){
            WaitingQuery waitingQuery = new WaitingQuery(helper.getLastRow(DNSQuery.class), dnsMessage.id);
            synchronized (waitingQueries){
                waitingQueries.add(waitingQuery);
            }
        }
    }

    public void logUpstreamAnswer(DNSMessage dnsMessage){
        if(!logUpstreamAnswers || dnsMessage.answerSection.size() == 0)return;
        WaitingQuery waitingQuery;
        for (int i = 0; i < waitingQueries.size(); i++) {
            String answer = null;
            waitingQuery = waitingQueries.get(i);
            if(waitingQuery.messageID == dnsMessage.id){
                answer = getAnswer(dnsMessage);
            }else if(dnsMessage.getQuestion() != null && waitingQuery.query.getHost().equalsIgnoreCase(dnsMessage.getQuestion().name.ace)){
                answer = getAnswer(dnsMessage);
            }
            if(answer != null) {
                waitingQuery.query.setUpstreamAnswer(answer);
                helper.update(waitingQuery.query);
                synchronized (waitingQueries){
                    waitingQueries.remove(i); //This is possible because we only add to the end of the list
                }
                break;
            }
        }
    }

    @Nullable
    private String getAnswer(@NonNull DNSMessage message){
        if(message.answerSection.size() == 0)return null;
        for (Record<? extends Data> record : message.answerSection) {
            if(record.type == Record.TYPE.A || record.type == Record.TYPE.AAAA) return record.payloadData.toString();
        }
        return null;
    }

    public boolean logUpstreamAnswers() {
        return logUpstreamAnswers;
    }

    public static void setNewQueryLoggedCallback(Runnable runnable){
        newQueryLogged = runnable;
    }

    public void destroy(){
        helper = null;
        newQueryLogged = null;
        waitingQueries.clear();
    }

    private class WaitingQuery{
        private DNSQuery query;
        private int messageID;

        public WaitingQuery(DNSQuery query, int messageID) {
            this.query = query;
            this.messageID = messageID;
        }
    }
}
