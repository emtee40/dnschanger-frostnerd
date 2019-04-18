package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.services.RuleImportService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.design.dialogs.LoadingDialog;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.general.StringUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.general.VariableChecker;
import com.frostnerd.utils.lifecyclehelper.UtilityActivity;

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
public class PinActivity extends UtilityActivity {
    private MaterialEditText met;
    private EditText pinInput;
    private String pin;
    private Vibrator vibrator;
    private static final String LOG_TAG = "[PinActivity]";
    private ImageView fingerprintImage;
    private Handler handler;
    private BroadcastReceiver importFinishedReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(ThemeHandler.getDialogTheme(this));
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity", getIntent());
        final boolean main = getIntent() != null && !getIntent().hasExtra("redirectToService");
        LogFactory.writeMessage(this, LOG_TAG, "Returning to main after pin: " + main);
        if(Utils.isServiceRunning(this, RuleImportService.class)){
            showRulesImportingDialog(main);
            return;
        }
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
                if(isFinishing())return;
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
                final FingerprintManager fingerprintManager = Utils.requireNonNull((FingerprintManager) getSystemService(FINGERPRINT_SERVICE));
                KeyguardManager keyguardManager = Utils.requireNonNull(getSystemService(KeyguardManager.class));
                fingerprintImage = findViewById(R.id.image);
                if(fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints() && keyguardManager.isKeyguardSecure()) {
                    handler = new Handler();
                    final int color = ThemeHandler.getColor(this, android.R.attr.textColor, 0);
                    fingerprintManager.authenticate(null, new CancellationSignal(), 0, new FingerprintManager.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                            if(isFinishing())return;
                            met.setIndicatorState(MaterialEditText.IndicatorState.CORRECT);
                            continueToFollowing(main);
                            fingerprintImage.setImageDrawable(DesignUtil.setDrawableColor
                                    (DesignUtil.getDrawable(PinActivity.this, R.drawable.ic_fingerprint), Color.GREEN));
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            if(isFinishing())return;
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

    private void showRulesImportingDialog(final boolean continueToMain){
        final LoadingDialog loadingDialog = new LoadingDialog(this, ThemeHandler.getDialogTheme(this),
                getString(R.string.loading),
                getString(R.string.info_importing_rules_app_unusable));
        loadingDialog.setCancelable(false);
        loadingDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.background), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
        registerReceiver(importFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadingDialog.dismiss();
                unregisterReceiver(this);
                importFinishedReceiver = null;
                continueToFollowing(continueToMain);
            }
        }, new IntentFilter(RuleImportService.BROADCAST_IMPORT_FINISHED));
    }

    @Override
    protected void onStop() {
        if(!isFinishing()) finish();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogFactory.writeMessage(this, LOG_TAG, "Destroying activity");
        if(importFinishedReceiver != null)unregisterReceiver(importFinishedReceiver);
        if(handler != null) handler.removeCallbacksAndMessages(null);
        importFinishedReceiver = null;
        met = null;
        pinInput = null;
        vibrator = null;
        fingerprintImage = null;
        handler = null;
        super.onDestroy();
    }

    @Override
    protected Configuration getConfiguration() {
        return Configuration.withDefaults().setDismissFragmentsOnPause(true);
    }
}
