package com.frostnerd.dnschanger.activities;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.frostnerd.api.dnschanger.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.dialogs.DNSEntryListDialog;
import com.frostnerd.dnschanger.util.ThemeHandler;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class BackgroundDNSListActivity extends AppCompatActivity {
    public static final String KEY_MESSAGE = "MESSAGE";
    private Message message;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeHandler.getDialogTheme(this));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        if(getIntent() == null)
            throw new IllegalStateException();
        if(!getIntent().hasExtra(KEY_MESSAGE))
            throw new IllegalStateException();
        if(getIntent().getParcelableExtra(KEY_MESSAGE) == null)
            throw new IllegalStateException();

        message = getIntent().getParcelableExtra(KEY_MESSAGE);
        new DNSEntryListDialog(this, ThemeHandler.getDialogTheme(this), new DNSEntryListDialog.OnProviderSelectedListener() {
            @Override
            public void onProviderSelected(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6) {
                Message m = Message.obtain();
                Bundle b = new Bundle();
                new DNSEntry(dns1.toString(true), dns2.toString(true),
                        dns1V6.toString(true), dns2V6.toString(true), name).writeToBundle(b);
                m.obj = b;
                try {
                    message.replyTo.send(m);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                finish();
            }
        }).show();
    }
}
