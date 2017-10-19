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
import android.widget.Button;
import android.widget.EditText;

import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.networking.NetworkUtil;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
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
    private Mode mode;
    private DNSEntry editedEntry;

    public DNSCreationDialog(@NonNull Context context, @NonNull final OnEditingFinishedListener listener, final DNSEntry entry) {
        this(context, new OnCreationFinishedListener() {
            @Override
            public void onCreationFinished(String name, String dns1, String dns2, String dns1V6, String dns2V6) {
                DNSEntry newEntry = new DNSEntry(entry.getID(), name, name, dns1, dns2, dns1V6, dns2V6, entry.getDescription(), entry.isCustomEntry());
                listener.editingFinished(newEntry);
            }
        });
        mode = Mode.EDITING;
        editedEntry = entry;
        setTitle(R.string.edit);
        dns1 = entry.getDns1();
        dns2 = entry.getDns2();
        dns1V6 = entry.getDns1V6();
        dns2V6 = entry.getDns2V6();
        ed_dns1.setText(settingV6 ? dns1V6 : dns1);
        ed_dns2.setText(settingV6 ? dns2V6 : dns2);
        ed_name.setText(entry.getName());
    }

    public DNSCreationDialog(@NonNull Context context, @NonNull final OnCreationFinishedListener listener) {
        super(context, ThemeHandler.getDialogTheme(context));
        this.mode = Mode.CREATION;
        setView(view = LayoutInflater.from(context).inflate(R.layout.dialog_create_dns_entry, null, false));
        final boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(context),
                ipv6Enabled = !ipv4Enabled || PreferencesAccessor.isIPv6Enabled(context);
        settingV6 = !ipv4Enabled;
        ed_dns1 = view.findViewById(R.id.dns1);
        ed_dns2 = view.findViewById(R.id.dns2);
        ed_name = view.findViewById(R.id.name);
        met_name = view.findViewById(R.id.met_name);
        met_dns1 = view.findViewById(R.id.met_dns1);
        met_dns2 = view.findViewById(R.id.met_dns2);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        ed_dns1.setText(settingV6 ? dns1V6 : dns1);
        ed_dns2.setText(settingV6 ? dns2V6 : dns2);
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
        if(ipv6Enabled && ipv4Enabled)setButton(BUTTON_NEUTRAL, "V6", (OnClickListener) null);
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
                if(ipv6Enabled && ipv4Enabled)getButton(BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settingV6 = !settingV6;
                        ed_dns1.setText(settingV6 ? dns1V6 : dns1);
                        ed_dns2.setText(settingV6 ? dns2V6 : dns2);
                        ((Button) v).setText(settingV6 ? "V4" : "V6");
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
                (!PreferencesAccessor.isIPv6Enabled(getContext()) || (NetworkUtil.isAssignableAddress(dns1V6, true, false) &&
                        NetworkUtil.isAssignableAddress(dns2V6, true, true))) && met_name.getIndicatorState() == MaterialEditText.IndicatorState.CORRECT;
    }

    public static interface OnCreationFinishedListener {
        public void onCreationFinished(String name, String dns1, String dns2, String dns1V6, String dns2V6);
    }

    public static interface OnEditingFinishedListener{
        public void editingFinished(DNSEntry entry);
    }

    public enum Mode{
        CREATION,EDITING
    }
}
