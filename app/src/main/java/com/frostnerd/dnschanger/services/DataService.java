package com.frostnerd.dnschanger.services;

import android.content.Intent;
import android.os.Message;
import android.os.RemoteException;

import com.frostnerd.api.DataExchangeService;
import com.frostnerd.api.DataExchanger;
import com.frostnerd.api.dataexchangers.PreferencesExchanger;
import com.frostnerd.dnschanger.activities.BackgroundDNSListActivity;
import com.frostnerd.dnschanger.util.Preferences;
/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class DataService extends DataExchangeService {
    public static final int ARG_CHOOSE_SERVER = 1;

    public boolean handleMessage(final Message message) {
        if (message.replyTo != null) {
            if (message.arg1 == ARG_CHOOSE_SERVER) {
                startActivity(new Intent(this, BackgroundDNSListActivity.class).
                        putExtra(BackgroundDNSListActivity.KEY_MESSAGE, message));
            }
        } else {
            try {
                DataExchanger.executeExchangersAndSendAnswers(Preferences.getInstance(this), message,
                        message.replyTo, PreferencesExchanger.class);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
