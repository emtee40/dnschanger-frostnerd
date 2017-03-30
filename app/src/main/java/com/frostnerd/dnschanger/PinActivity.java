package com.frostnerd.dnschanger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.EditText;

import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class PinActivity extends Activity {
    private MaterialEditText met;
    private EditText pinInput;
    private String pin;
    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean main = getIntent() != null && !getIntent().hasExtra("redirectToService");
        if(!Preferences.getBoolean(this, "setting_pin_enabled", false)){
            continueToFollowing(main);
            return;
        }
        if((main && !Preferences.getBoolean(this, "pin_app", false)) ||
                (!main && !Preferences.getBoolean(this, "pin_notification",false)))continueToFollowing(main);
        setContentView(R.layout.pin_dialog);
        pin = Preferences.getString(this, "pin_value", "1234");
        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        met = (MaterialEditText)findViewById(R.id.pin_dialog_met);
        pinInput = (EditText)findViewById(R.id.pin_dialog_pin);
        findViewById(R.id.pin_dialog_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.pin_dialog_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pinInput.getText().toString().equals(pin)){
                    met.setIndicatorState(MaterialEditText.IndicatorState.CORRECT);
                    continueToFollowing(main);
                }else{
                    met.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                    vibrator.vibrate(200);
                }
            }
        });
    }

    private void continueToFollowing(boolean toMain){
        if(toMain)startActivity(new Intent(this, MainActivity.class));
        else startService(new Intent(this, DNSVpnService.class).
                putExtra("start_vpn", getIntent().getBooleanExtra("start_vpn",false)).
                putExtra("stop_vpn", getIntent().getBooleanExtra("stop_vpn",false)).
                putExtra("destroy", getIntent().getBooleanExtra("destroy",false)));
        finish();
    }
}
