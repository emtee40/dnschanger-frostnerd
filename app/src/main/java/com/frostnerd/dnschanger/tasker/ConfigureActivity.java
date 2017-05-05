package com.frostnerd.dnschanger.tasker;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.ShortcutActivity;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class ConfigureActivity extends AppCompatActivity {
    private MaterialEditText met_dns1, met_dns2, met_name;
    private EditText ed_dns1, ed_dns2, ed_name;
    private boolean cancelled = false, creatingShortcut;
    private DefaultDNSDialog defaultDNSDialog;
    private String dns1 = "8.8.8.8", dns2 = "8.8.4.4", dns1V6 ="2001:4860:4860::8888", dns2V6 = "2001:4860:4860::8844";
    private boolean settingV6 = false, wasEdited = false;
    private long lastBackPress = 0;
    private Action currentAction;
    private static final String LOG_TAG = "[ConfigureActivity]";

    private enum Action{
        PAUSE, START, STOP, RESUME
    }

    private boolean checkValidity(){
        return wasEdited && NetworkUtil.isAssignableAddress(dns1,false,false) && NetworkUtil.isAssignableAddress(dns2,false,true) &&
                (!API.isIPv6Enabled(this) || (NetworkUtil.isAssignableAddress(dns1V6,true,false) &&
                NetworkUtil.isAssignableAddress(dns2V6,true,true))) && met_name.getIndicatorState() == MaterialEditText.IndicatorState.CORRECT;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasker_configure_layout);
        LogFactory.writeMessage(this, LOG_TAG, "Activity created", getIntent());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ed_dns1 = (EditText)findViewById(R.id.dns1);
        ed_dns2 = (EditText)findViewById(R.id.dns2);
        ed_name = (EditText)findViewById(R.id.name);
        met_dns1 = (MaterialEditText) findViewById(R.id.met_dns1);
        met_dns2 = (MaterialEditText)findViewById(R.id.met_dns2);
        met_name = (MaterialEditText)findViewById(R.id.met_name);
        Spinner actionSpinner = (Spinner) findViewById(R.id.spinner);
        Helper.scrub(getIntent());
        final Bundle bundle = getIntent().getBundleExtra(Helper.EXTRA_BUNDLE);
        creatingShortcut = getIntent() != null && getIntent().getBooleanExtra("creatingShortcut", false);
        LogFactory.writeMessage(this, LOG_TAG, "Creating Shortcut: " + creatingShortcut);
        Helper.scrub(bundle);
        if(savedInstanceState == null){
            if(Helper.isBundleValid(bundle)){
                LogFactory.writeMessage(this, LOG_TAG, "Editing existing Tasker Configuration");
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_DNS1))dns1 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1);
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_DNS2))dns2 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2);
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_DNS1V6))dns1V6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6);
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_DNS2V6))dns2V6 = bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6);
                if(getIntent().hasExtra(Helper.EXTRA_BLURB))ed_name.setText(getIntent().getStringExtra(Helper.EXTRA_BLURB));
            }
        }
        ed_dns1.setText(dns1);
        ed_dns2.setText(dns2);
        ed_dns1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!NetworkUtil.isAssignableAddress(s.toString(),settingV6, false)) {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    wasEdited = true;
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if(settingV6)dns1V6 = s.toString();
                    else dns1 = s.toString();
                }
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
                if (!NetworkUtil.isAssignableAddress(s.toString(),settingV6, false)) {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    wasEdited = true;
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if(settingV6)dns2V6 = s.toString();
                    else dns2 = s.toString();
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
                wasEdited = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        if(!creatingShortcut && Preferences.getBoolean(this, "setting_auto_wifi",false) || Preferences.getBoolean(this, "setting_auto_mobile",false)
                || Preferences.getBoolean(this, "setting_start_boot", false) || Preferences.getBoolean(this, "setting_auto_disable",false)){
            new AlertDialog.Builder(this).setTitle(R.string.warning).setMessage(creatingShortcut ? R.string.shortcut_conflict_text : R.string.tasker_automation_conflict_text).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Preferences.put(ConfigureActivity.this, "setting_auto_wifi", false);
                    Preferences.put(ConfigureActivity.this, "setting_auto_mobile", false);
                    Preferences.put(ConfigureActivity.this, "setting_start_boot", false);
                    Preferences.put(ConfigureActivity.this, "setting_auto_disable", false);
                }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).show();
        }
        ((TextView)findViewById(R.id.text)).setText(creatingShortcut ? R.string.create_shortcut : R.string.create_tasker_action);
        ed_name.requestFocus();
        if(creatingShortcut) actionSpinner.setVisibility(View.GONE);
        else{
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.tasker_configure_actions, R.layout.tasker_action_spinner_item);
            adapter.setDropDownViewResource(R.layout.tasker_action_spinner_dropdown_item);
            actionSpinner.setAdapter(adapter);
            actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if(position != 0)findViewById(R.id.wrapper).setVisibility(View.INVISIBLE);
                    else findViewById(R.id.wrapper).setVisibility(View.VISIBLE);
                    currentAction = position == 0 ? Action.START : (position == 1 ? Action.STOP : (position == 2 ? Action.PAUSE : Action.RESUME));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            actionSpinner.getBackground().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP);
        }
        findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelled = true;
                finish();
            }
        });
    }

    public void openDefaultDNSDialog(View v){
        defaultDNSDialog = new DefaultDNSDialog(this, ThemeHandler.getDialogTheme(this), new DefaultDNSDialog.OnProviderSelectedListener() {
            @Override
            public void onProviderSelected(String name, String dns1, String dns2, String dns1V6, String dns2V6) {
                ConfigureActivity.this.dns1 = dns1.equals("") ? ConfigureActivity.this.dns1 : dns1;
                ConfigureActivity.this.dns2 = dns2.equals("") ? ConfigureActivity.this.dns2 : dns2;
                ConfigureActivity.this.dns1V6 = dns1V6.equals("") ? ConfigureActivity.this.dns1V6 : dns1V6;
                ConfigureActivity.this.dns2V6 = dns2V6.equals("") ? ConfigureActivity.this.dns2V6 : dns2V6;
                ed_dns1.setText(settingV6 ? dns1V6 : dns1);
                ed_dns2.setText(settingV6 ? dns2V6 : dns2);
            }
        });
        defaultDNSDialog.show();
    }

    @Override
    protected void onDestroy() {
        if(defaultDNSDialog != null)defaultDNSDialog.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(API.isIPv6Enabled(this) ? (settingV6 ? R.menu.tasker_menu_v4 : R.menu.tasker_menu_v6) : R.menu.tasker_menu_no_ipv6 ,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_switch_ip_version){
            settingV6 = !settingV6;
            invalidateOptionsMenu();
            ed_dns1.setText(settingV6 ? dns1V6 : dns1);
            ed_dns2.setText(settingV6 ? dns2V6 : dns2);
            ed_dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            ed_dns2.setInputType(InputType.TYPE_CLASS_TEXT);
            getSupportActionBar().setSubtitle(getString(R.string.subtitle_configuring).replace("[[x]]",settingV6 ? "Ipv6" : "Ipv4"));
        }else if(item.getItemId() == android.R.id.home){
            lastBackPress = System.currentTimeMillis();
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(creatingShortcut)super.onBackPressed();
        else{
            if(System.currentTimeMillis() - lastBackPress <= 1500){
                cancelled = true;
                super.onBackPressed();
            }else{
                lastBackPress = System.currentTimeMillis();
                Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void finish() {
        LogFactory.writeMessage(this, LOG_TAG, "Activity finished");
        if(!cancelled && checkValidity() && !creatingShortcut){
            LogFactory.writeMessage(this, LOG_TAG, "Not cancelled, inputs valid, not creating shortcut");
            if(currentAction == Action.START){
                LogFactory.writeMessage(this, LOG_TAG, "Action is START");
                final Intent resultIntent = new Intent();
                final Bundle resultBundle = Helper.createBundle(dns1, dns2, dns1V6, dns2V6);
                resultIntent.putExtra(Helper.EXTRA_BUNDLE, resultBundle);
                resultIntent.putExtra(Helper.EXTRA_BLURB, ed_name.getText().toString());
                LogFactory.writeMessage(this, LOG_TAG, "Bundle created", resultIntent);
                setResult(RESULT_OK, resultIntent);
            }else{
                LogFactory.writeMessage(this, LOG_TAG, "Acttion is other than START");
                if(ed_name.getText().toString().equals("")){
                    LogFactory.writeMessage(this, LOG_TAG, "Name is emtpy. Configurating cancelled");
                    setResult(RESULT_CANCELED);
                }else{
                    final Intent resultIntent = new Intent();
                    final Bundle resultBundle = new Bundle();
                    if(currentAction == Action.PAUSE){
                        LogFactory.writeMessage(this, LOG_TAG, "Action is PAUSE");
                        resultBundle.putBoolean(Helper.BUNDLE_EXTRA_PAUSE_DNS,true);
                    }else if(currentAction == Action.RESUME){
                        LogFactory.writeMessage(this, LOG_TAG, "Action is RESUME");
                        resultBundle.putBoolean(Helper.BUNDLE_EXTRA_RESUME_DNS,true);
                    }else if(currentAction == Action.STOP){
                        LogFactory.writeMessage(this, LOG_TAG, "Action is STOP");
                        resultBundle.putBoolean(Helper.BUNDLE_EXTRA_STOP_DNS,true);
                    }
                    resultIntent.putExtra(Helper.EXTRA_BUNDLE, resultBundle);
                    resultIntent.putExtra(Helper.EXTRA_BLURB, ed_name.getText().toString());
                    LogFactory.writeMessage(this, LOG_TAG, "Bundle created", resultIntent);
                    setResult(RESULT_OK, resultIntent);
                }
            }
        }else if(!cancelled && checkValidity() && creatingShortcut){
            LogFactory.writeMessage(this, LOG_TAG, "Cancelled, valid, creating shortcut");
            Intent shortcutIntent = new Intent(getBaseContext(), ShortcutActivity.class);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            shortcutIntent.putExtra("dns1", dns1);
            shortcutIntent.putExtra("dns2", dns2);
            shortcutIntent.putExtra("dns1v6", dns1V6);
            shortcutIntent.putExtra("dns2v6", dns2V6);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, ed_name.getText().toString());
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.mipmap.ic_launcher));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            LogFactory.writeMessage(this, LOG_TAG, "Adding shortcut", shortcutIntent);
            LogFactory.writeMessage(this, LOG_TAG, "Intent for adding to Screen:", addIntent);
            getApplicationContext().sendBroadcast(addIntent);
            setResult(RESULT_OK);
            LogFactory.writeMessage(this, LOG_TAG, "Shortcut added to Launcher");
            API.onShortcutCreated(this, dns1, dns2, dns1V6, dns2V6, ed_name.getText().toString());
        }
        super.finish();
    }
}
