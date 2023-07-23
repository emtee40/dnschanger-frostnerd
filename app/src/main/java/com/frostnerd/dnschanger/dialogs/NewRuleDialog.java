package com.frostnerd.dnschanger.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.util.RuleImport;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.lifecycle.BaseDialog;
import com.frostnerd.materialedittext.MaterialEditText;
import com.frostnerd.networking.NetworkUtil;

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
public class NewRuleDialog extends BaseDialog {
    private MaterialEditText metHost;
    private MaterialEditText metTarget;
    private MaterialEditText metTarget2;
    private EditText edHost;
    private EditText edTarget;
    private EditText edTarget2;
    private CheckBox wildcard;
    private RadioButton ipv6;
    private RadioButton ipv4;
    private RadioButton both;
    private RadioGroup addressType;
    private Vibrator vibrator;
    private String v6Text = "::1", v4Text = "127.0.0.1";
    private boolean editingMode = false;
    private RuleImportChooserDialog ruleImportChooserDialog;

    public <T extends Activity &RuleImport.ImportStartedListener> NewRuleDialog(@NonNull T context, final CreationListener listener, @NonNull final String host, @NonNull String target, final boolean wildcard, final boolean ipv6){
        this(context, listener);
        if(ipv6)v6Text = target;
        else v4Text = target;
        edHost.setText(host);
        edHost.setEnabled(false);
        edTarget.setText(target);
        both.setVisibility(View.GONE);
        this.ipv6.setChecked(ipv6);
        this.wildcard.setChecked(wildcard);
        this.addressType.setEnabled(false);
        this.wildcard.setEnabled(false);
        this.ipv6.setEnabled(false);
        this.ipv4.setEnabled(false);
        editingMode = true;
        setButton(BUTTON_NEGATIVE, context.getString(R.string.delete), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.creationFinished(host, null, null, ipv6, wildcard, true);
            }
        });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if(ruleImportChooserDialog != null)ruleImportChooserDialog.dismiss();
            }
        });
    }

    public <T extends Activity&RuleImport.ImportStartedListener> NewRuleDialog(@NonNull final T context, final CreationListener listener) {
        super(context, ThemeHandler.getDialogTheme(context));
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_new_rule, null, false));
        metHost = content.findViewById(R.id.met_host);
        metTarget = content.findViewById(R.id.met_target);
        metTarget2 = content.findViewById(R.id.met_target2);
        edHost = content.findViewById(R.id.host);
        edTarget = content.findViewById(R.id.target);
        edTarget2 = content.findViewById(R.id.target2);
        metHost = content.findViewById(R.id.met_host);
        wildcard = content.findViewById(R.id.wildcard);
        ipv6 = content.findViewById(R.id.radio_ipv6);
        ipv4 = content.findViewById(R.id.radio_ipv4);
        both = content.findViewById(R.id.radio_both);
        addressType = content.findViewById(R.id.group);
        setTitle(R.string.new_rule);
        setCancelable(true);
        setButton(BUTTON_NEUTRAL, context.getString(R.string.close), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setButton(BUTTON_NEGATIVE, getContext().getString(R.string.import_rules), (OnClickListener)null);
        setButton(BUTTON_POSITIVE, context.getString(R.string.done), (OnClickListener)null);
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(inputsValid()){
                            if(editingMode){
                                listener.creationFinished(edHost.getText().toString(), edTarget.getText().toString(), null, ipv6.isChecked(), wildcard.isChecked(), true);
                                dismiss();
                            }else{
                                if(both.isChecked() && !DatabaseHelper.getInstance(getContext()).dnsRuleExists(edHost.getText().toString())){
                                    listener.creationFinished(edHost.getText().toString(),
                                            edTarget.getText().toString(), edTarget2.getText().toString(),
                                            ipv6.isChecked(), wildcard.isChecked(), false);
                                    dismiss();
                                }else if(!DatabaseHelper.getInstance(getContext()).dnsRuleExists(edHost.getText().toString(), ipv6.isChecked())){
                                    listener.creationFinished(edHost.getText().toString(),
                                            edTarget.getText().toString(), both.isChecked() ? edTarget2.getText().toString() : "",
                                            ipv6.isChecked(), wildcard.isChecked(), false);
                                    dismiss();
                                }else{
                                    Toast.makeText(getContext(), R.string.error_rule_already_exists, Toast.LENGTH_LONG).show();
                                }
                            }
                        }else{
                            vibrator.vibrate(200);
                        }
                    }
                });
                if(!editingMode)getButton(BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                        ruleImportChooserDialog =  new RuleImportChooserDialog(context);
                        ruleImportChooserDialog.show();
                    }
                });
            }
        });
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInput();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        edHost.addTextChangedListener(textWatcher);
        edTarget.addTextChangedListener(textWatcher);
        edTarget2.addTextChangedListener(textWatcher);
        addressType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                setTargetValues();
                metTarget.setLabelText(ipv6.isChecked() ? "IPv6" : "IPv4");
                metTarget.setIcon(ipv6.isChecked() ? R.drawable.ic_action_ipv6 : R.drawable.ic_action_ipv4);
                metTarget2.setVisibility(both.isChecked() ? View.VISIBLE : View.GONE);
                validateInput();
            }
        });
        wildcard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                metHost.setLabelText(context.getString(isChecked ? R.string.regular_expression : R.string.host));
            }
        });
    }

    @Override
    protected void destroy() {
        metHost = metTarget = metTarget2 = null;
        edHost = edTarget = edTarget2 = null;
        wildcard = null;
        ipv4 = ipv6 = both = null;
        addressType = null;
        vibrator = null;
        ruleImportChooserDialog = null;
    }

    private void setTargetValues(){
        if(metTarget2.getVisibility() == View.VISIBLE)v6Text = edTarget2.getText().toString();
        if(ipv6.isChecked()){
            v4Text = edTarget.getText().toString();
            edTarget.setText(v6Text);
        }else if(ipv4.isChecked()){
            v6Text = metTarget2.getVisibility() == View.VISIBLE ? edTarget2.getText().toString() : edTarget.getText().toString();
            edTarget.setText(v4Text);
        }else{
            edTarget.setText(v4Text);
            edTarget2.setText(v6Text);
        }
    }

    private boolean inputsValid(){
        return metHost.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED &&
                metTarget.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED &&
                (metTarget2.getVisibility() == View.GONE || metTarget2.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED);
    }

    private void validateInput(){
        metHost.setIndicatorState(!edHost.getText().toString().equals("")
                ? MaterialEditText.IndicatorState.UNDEFINED : MaterialEditText.IndicatorState.INCORRECT);
        metTarget.setIndicatorState(NetworkUtil.isIP(edTarget.getText().toString(), ipv6.isChecked())
                ? MaterialEditText.IndicatorState.UNDEFINED : MaterialEditText.IndicatorState.INCORRECT);
        metTarget2.setIndicatorState(NetworkUtil.isIP(edTarget2.getText().toString(), true) && !edTarget2.getText().toString().equals("")
                ? MaterialEditText.IndicatorState.UNDEFINED : MaterialEditText.IndicatorState.INCORRECT);
        if(getButton(BUTTON_POSITIVE) != null)getButton(BUTTON_POSITIVE).setEnabled(inputsValid());
    }


    public interface CreationListener{
        void creationFinished(@NonNull String host, @Nullable String target, @Nullable String targetV6, boolean ipv6, boolean wildcard, boolean wasEdited);
    }
}
