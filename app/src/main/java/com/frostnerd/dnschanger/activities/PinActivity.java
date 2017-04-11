package com.frostnerd.dnschanger.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class PinActivity extends Activity {
    private MaterialEditText met;
    private EditText pinInput;
    private String pin;
    private Vibrator vibrator;
    private static final String LOG_TAG = "[PinActivity]";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity", getIntent());
        final boolean main = getIntent() != null && !getIntent().hasExtra("redirectToService");
        LogFactory.writeMessage(this, LOG_TAG, "Returning to main after pin: " + main);
        if (!Preferences.getBoolean(this, "setting_pin_enabled", false)) {
            LogFactory.writeMessage(this, LOG_TAG, "Pin is disabled");
            continueToFollowing(main);
            return;
        }
        if ((main && !Preferences.getBoolean(this, "pin_app", false)) ||
                (!main && (!Preferences.getBoolean(this, "pin_notification", false) && !Preferences.getBoolean(this, "pin_tile", false)))) {
            if (main && !Preferences.getBoolean(this, "pin_app", false)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are going to main and pin for the app is not enabled. Not asking for pin");
            } else if (!main && !Preferences.getBoolean(this, "pin_notification", false)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are doing something in the notification and pin for it is not enabled. Not asking for pin");
            } else if (!main && !Preferences.getBoolean(this, "pin_tile", false)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are doing something in the tiles and pin for it is not enabled. Not asking for pin");
            }
            continueToFollowing(main);
        }
        LogFactory.writeMessage(this, LOG_TAG, "Have to ask for pin.");
        setContentView(R.layout.pin_dialog);
        LogFactory.writeMessage(this, LOG_TAG, "Content set");
        pin = Preferences.getString(this, "pin_value", "1234");
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        met = (MaterialEditText) findViewById(R.id.pin_dialog_met);
        pinInput = (EditText) findViewById(R.id.pin_dialog_pin);
        findViewById(R.id.pin_dialog_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogFactory.writeMessage(PinActivity.this, LOG_TAG, "Cancelling pin Input");
                finish();
            }
        });
        findViewById(R.id.pin_dialog_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pinInput.getText().toString().equals(pin)) {
                    LogFactory.writeMessage(PinActivity.this, LOG_TAG, "Correct pin entered");
                    met.setIndicatorState(MaterialEditText.IndicatorState.CORRECT);
                    continueToFollowing(main);
                } else {
                    LogFactory.writeMessage(PinActivity.this, LOG_TAG, "Incorrect pin entered");
                    met.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                    vibrator.vibrate(200);
                }
            }
        });
        LogFactory.writeMessage(this, LOG_TAG, "Activity fully created.");
    }

    private void continueToFollowing(boolean toMain) {
        LogFactory.writeMessage(this, LOG_TAG, "Trying to continue to following window/action");
        if (toMain) {
            Intent i;
            LogFactory.writeMessage(this, LOG_TAG, "Starting MainActivity",
                    i = new Intent(this, MainActivity.class));
            startActivity(i);
        } else {
            Intent i;
            LogFactory.writeMessage(this, LOG_TAG, "Starting DNSVPNService",
                    i = new Intent(this, DNSVpnService.class).
                            putExtra("start_vpn", getIntent().getBooleanExtra("start_vpn", false)).
                            putExtra("stop_vpn", getIntent().getBooleanExtra("stop_vpn", false)).
                            putExtra("destroy", getIntent().getBooleanExtra("destroy", false)));
            startService(i);
            LogFactory.writeMessage(this, LOG_TAG, "Service Started");
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        LogFactory.writeMessage(this, LOG_TAG, "Destroying activity");
        super.onDestroy();
    }
}
