package com.frostnerd.dnschanger.services;

import android.content.Intent;
import android.os.Message;
import android.os.RemoteException;

import com.frostnerd.api.DataExchangeService;
import com.frostnerd.api.DataExchanger;
import com.frostnerd.api.dataexchangers.PreferencesExchanger;
import com.frostnerd.dnschanger.activities.BackgroundDNSListActivity;
import com.frostnerd.dnschanger.util.Preferences;
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
        return true;
    }
}
