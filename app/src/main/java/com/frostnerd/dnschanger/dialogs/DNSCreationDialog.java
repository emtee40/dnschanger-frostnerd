package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.general.textfilers.InputCharacterFilter;
import com.frostnerd.lifecycle.BaseDialog;
import com.frostnerd.materialedittext.MaterialEditText;

import java.util.regex.Pattern;

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
public class DNSCreationDialog extends BaseDialog {
    private IPPortPair dns1, dns2 ,
            dns1V6, dns2V6;
    private EditText ed_dns1, ed_dns2;
    private EditText ed_name;
    private MaterialEditText met_name, met_dns1, met_dns2;
    private Vibrator vibrator;
    private boolean settingV6;
    private Pattern namePattern = Pattern.compile("[^'#´`~]+");
    private final boolean customPorts;
    {
        customPorts = PreferencesAccessor.areCustomPortsEnabled(getContext());
    }

    public DNSCreationDialog(@NonNull Context context, @NonNull final OnEditingFinishedListener listener, final DNSEntry entry) {
        this(context, new OnCreationFinishedListener() {
            @Override
            public void onCreationFinished(@NonNull String name, @NonNull IPPortPair dns1, IPPortPair dns2, @NonNull IPPortPair dns1V6, IPPortPair dns2V6) {
                entry.setDns1(dns1);
                entry.setDns2(dns2);
                entry.setDns1V6(dns1V6);
                entry.setDns2V6(dns2V6);
                entry.setName(name);
                entry.setShortName(name);
                listener.editingFinished(entry);
            }
        });
        setTitle(R.string.edit);
        dns1 = entry.getDns1();
        dns2 = entry.getDns2();
        dns1V6 = entry.getDns1V6();
        dns2V6 = entry.getDns2V6();
        ed_dns1.setText(settingV6 ? dns1V6.toString(customPorts) : dns1.toString(customPorts));
        ed_dns2.setText(settingV6 ? dns2V6.toString(customPorts) : dns2.toString(customPorts));
        ed_name.setText(entry.getName());
    }

    @Override
    protected void destroy() {
        ed_dns1 = ed_dns2 = ed_name = null;
        met_dns1 = met_dns2 = met_name = null;
        vibrator = null;
    }

    public DNSCreationDialog(@NonNull final Context context, @NonNull final OnCreationFinishedListener listener) {
        super(context, ThemeHandler.getDialogTheme(context));
        dns1 = PreferencesAccessor.Type.DNS1.getPair(context);
        dns2 = PreferencesAccessor.Type.DNS2.getPair(context);
        dns1V6 = PreferencesAccessor.Type.DNS1_V6.getPair(context);
        dns2V6 = PreferencesAccessor.Type.DNS2_V6.getPair(context);
        View view;
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

        setEditTextStates();
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
                            listener.onCreationFinished(ed_name.getText().toString().trim(), dns1, dns2, dns1V6, dns2V6);
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
                        setEditTextStates();
                        ((Button) v).setText(settingV6 ? "V4" : "V6");
                    }
                });
            }
        });
        ed_dns1.addTextChangedListener(new TextWatcher() {
            private String before;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                before = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(this.before.equalsIgnoreCase(s.toString()))return;
                IPPortPair pair = Util.validateInput(s.toString(), settingV6, false,
                        PreferencesAccessor.isLoopbackAllowed(context), 53);
                if (pair == null || (pair.getPort() != 53 && pair.getPort() != 53 && !customPorts)) {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    if (pair.getPort() == -1) pair.setPort(53);
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if (settingV6) dns1V6 = pair;
                    else dns1 = pair;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        ed_dns2.addTextChangedListener(new TextWatcher() {
            private String before;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                before = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(this.before.equalsIgnoreCase(s.toString()))return;
                IPPortPair pair = Util.validateInput(s.toString(), settingV6, true,
                        PreferencesAccessor.isLoopbackAllowed(context), 53);
                if (pair == null || (pair != IPPortPair.getEmptyPair() && pair.getPort() != 53 && !customPorts)) {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    if (pair.getPort() == -1) pair.setPort(53);
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if (settingV6) dns2V6 = pair;
                    else dns2 = pair;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        ed_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(!namePattern.matcher(s.toString().trim()).matches()) met_name.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                else if(DatabaseHelper.getInstance(getContext()).dnsEntryExists(s.toString().trim())) met_name.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                else met_name.setIndicatorState(MaterialEditText.IndicatorState.CORRECT);
            }
        });
    }

    private boolean isConfigurationValid() {
        return dns1 != null && dns1V6 != null && ((PreferencesAccessor.isIPv4Enabled(getContext()) && !dns1.isEmpty()) ||
                (PreferencesAccessor.isIPv6Enabled(getContext()) && !dns1V6.isEmpty())) &&
                met_dns1.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED &&
                met_dns2.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED &&
                met_name.getIndicatorState() == MaterialEditText.IndicatorState.CORRECT;
    }

    private void setEditTextStates(){
        if(settingV6 || customPorts){
            ed_dns1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            ed_dns2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }
        if(settingV6){
            InputFilter filter = new InputCharacterFilter(customPorts ?
                    Pattern.compile("[0-9:a-f\\[\\]]") : Pattern.compile("[0-9:a-f]"));
            ed_dns2.setFilters(new InputFilter[]{filter});
            ed_dns1.setFilters(new InputFilter[]{filter});
        }else{
            InputFilter filter = new InputCharacterFilter(customPorts ?
                    Pattern.compile("[0-9.:]") : Pattern.compile("[0-9.]"));
            ed_dns2.setFilters(new InputFilter[]{filter});
            ed_dns1.setFilters(new InputFilter[]{filter});
        }
        ed_dns1.setText(settingV6 ? dns1V6.toString(customPorts) : dns1.toString(customPorts));
        ed_dns2.setText(settingV6 ? dns2V6.toString(customPorts) : dns2.toString(customPorts));
    }

    public interface OnCreationFinishedListener {
        void onCreationFinished(@NonNull String name, @NonNull IPPortPair dns1, @Nullable IPPortPair dns2,
                                @NonNull IPPortPair dns1V6, @Nullable IPPortPair dns2V6);
    }

    public interface OnEditingFinishedListener{
        void editingFinished(@NonNull DNSEntry entry);
    }

    public enum Mode{
        CREATION,EDITING
    }
}
