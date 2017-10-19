package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.general.VariableChecker;
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
    private ImageView fingerprintImage;
    private Handler handler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHandler.getDialogTheme(this));
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity", getIntent());
        final boolean main = getIntent() != null && !getIntent().hasExtra("redirectToService");
        LogFactory.writeMessage(this, LOG_TAG, "Returning to main after pin: " + main);
        if (!PreferencesAccessor.isPinProtectionEnabled(this)) {
            LogFactory.writeMessage(this, LOG_TAG, "Pin is disabled");
            continueToFollowing(main);
            return;
        }
        if ((main && !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP)) ||
                (!main && (!PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.NOTIFICATION) &&
                        !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.TILE) &&
                        !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP_SHORTCUT)))) {
            if (main && !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are going to main and pin for the app is not enabled. Not asking for pin");
            } else if (!main && !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.NOTIFICATION)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are doing something in the notification and pin for it is not enabled. Not asking for pin");
            } else if (!main && !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.TILE)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are doing something in the tiles and pin for it is not enabled. Not asking for pin");
            } else if (!main && !PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP_SHORTCUT)) {
                LogFactory.writeMessage(this, LOG_TAG, "We are doing something in an app shortcut and pin for it is not enabled. Not asking for pin");
            }
            continueToFollowing(main);
        }
        LogFactory.writeMessage(this, LOG_TAG, "Have to ask for pin.");
        setContentView(R.layout.dialog_pin);
        LogFactory.writeMessage(this, LOG_TAG, "Content set");
        pin = PreferencesAccessor.getPinCode(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        met = findViewById(R.id.pin_dialog_met);
        pinInput = findViewById(R.id.pin_dialog_pin);
        if (!VariableChecker.isInteger(pin))
            pinInput.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && PreferencesAccessor.canUseFingerprintForPin(this)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED){
                FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
                KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
                fingerprintImage = findViewById(R.id.image);
                if(fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints() && keyguardManager.isKeyguardSecure()) {
                    handler = new Handler();
                    final int color = ThemeHandler.getColor(this, android.R.attr.textColor, 0);
                    fingerprintManager.authenticate(null, new CancellationSignal(), 0, new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            met.setIndicatorState(MaterialEditText.IndicatorState.CORRECT);
                            continueToFollowing(main);
                            fingerprintImage.setImageDrawable(DesignUtil.setDrawableColor
                                    (DesignUtil.getDrawable(PinActivity.this, R.drawable.ic_fingerprint), Color.GREEN));
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            met.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                            vibrator.vibrate(200);
                            fingerprintImage.setImageDrawable(DesignUtil.setDrawableColor
                                    (DesignUtil.getDrawable(PinActivity.this, R.drawable.ic_fingerprint), Color.RED));
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    met.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                                    fingerprintImage.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(PinActivity.this, R.drawable.ic_fingerprint), color));
                                }
                            }, 3500);
                        }
                    }, null);
                    fingerprintImage.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_fingerprint), color));
                }
            }
        }
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
                            putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), getIntent().getBooleanExtra("start_vpn", false)).
                            putExtra(VPNServiceArgument.COMMAND_STOP_VPN.getArgument(), getIntent().getBooleanExtra("stop_vpn", false)).
                            putExtra(VPNServiceArgument.COMMAND_STOP_SERVICE.getArgument(), getIntent().getBooleanExtra("destroy", false)).
                            putExtra(VPNServiceArgument.ARGUMENT_STOP_REASON.getArgument(), getIntent().hasExtra("destroy") ? getIntent().getStringExtra("reason") : null).setAction(StringUtil.randomString(40)));
            Util.startService(this,i);
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
