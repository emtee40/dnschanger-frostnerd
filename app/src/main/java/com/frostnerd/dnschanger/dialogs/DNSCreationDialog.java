package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class DNSCreationDialog extends AlertDialog {
    private View view;
    private String dns1 = "8.8.8.8", dns2 = "8.8.4.4", dns1V6 = "2001:4860:4860::8888", dns2V6 = "2001:4860:4860::8844";
    private EditText ed_dns1, ed_dns2;
    private EditText ed_name;
    private MaterialEditText met_name, met_dns1, met_dns2;
    private Vibrator vibrator;
    private boolean settingV6;

    public DNSCreationDialog(@NonNull Context context, @NonNull final OnCreationFinishedListener listener) {
        super(context, ThemeHandler.getDialogTheme(context));
        setView(view = LayoutInflater.from(context).inflate(R.layout.dialog_create_dns_entry, null, false));
        ed_dns1 = (EditText) view.findViewById(R.id.dns1);
        ed_dns2 = (EditText) view.findViewById(R.id.dns2);
        ed_name = (EditText) view.findViewById(R.id.name);
        met_name = (MaterialEditText) view.findViewById(R.id.met_name);
        met_dns1 = (MaterialEditText) view.findViewById(R.id.met_dns1);
        met_dns2 = (MaterialEditText) view.findViewById(R.id.met_dns2);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        setTitle(R.string.new_entry);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setButton(BUTTON_POSITIVE, context.getString(R.string.done), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        if(Preferences.getBoolean(context, "setting_ipv6_enabled", true))setButton(BUTTON_NEUTRAL, "V6", (OnClickListener) null);
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isConfigurationValid()) {
                            listener.onCreationFinished(ed_name.getText().toString(), dns1, dns2, dns1V6, dns2V6);
                            dismiss();
                        } else {
                            vibrator.vibrate(300);
                        }
                    }
                });
                if(Preferences.getBoolean(getContext(), "setting_ipv6_enabled", true))getButton(BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settingV6 = !settingV6;
                        ed_dns1.setText(settingV6 ? dns1V6 : dns1);
                        ed_dns2.setText(settingV6 ? dns2V6 : dns2);
                    }
                });
            }
        });
        ed_dns1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (NetworkUtil.isAssignableAddress(s.toString(), settingV6, false)) {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if (settingV6) dns1V6 = s.toString();
                    else dns1 = s.toString();
                } else met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        ed_dns2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (NetworkUtil.isAssignableAddress(s.toString(), settingV6, true)) {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if (settingV6) dns2V6 = s.toString();
                    else dns2 = s.toString();
                } else met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private boolean isConfigurationValid() {
        return NetworkUtil.isAssignableAddress(dns1, false, false) && NetworkUtil.isAssignableAddress(dns2, false, true) &&
                (!API.isIPv6Enabled(getContext()) || (NetworkUtil.isAssignableAddress(dns1V6, true, false) &&
                        NetworkUtil.isAssignableAddress(dns2V6, true, true))) && met_name.getIndicatorState() == MaterialEditText.IndicatorState.CORRECT;
    }

    public static interface OnCreationFinishedListener {
        public void onCreationFinished(String name, String dns1, String dns2, String dns1V6, String dns2V6);
    }
}
