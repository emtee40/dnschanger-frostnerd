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
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.utils.textfilers.InputCharacterFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

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
    private IPPortPair dns1 = IPPortPair.wrap("8.8.8.8" ,53), dns2 = IPPortPair.wrap("8.8.4.4", 53),
            dns1V6 = IPPortPair.wrap("2001:4860:4860::8888", 53), dns2V6 = IPPortPair.wrap("2001:4860:4860::8844", 53);
    private boolean settingV6 = false, wasEdited = false, ipv4Enabled, ipv6Enabled;
    private long lastBackPress = 0;
    private Action currentAction = Action.START;
    private static final String LOG_TAG = "[ConfigureActivity]";
    private boolean customPorts;

    private enum Action{
        PAUSE(2), START(0), STOP(1), RESUME(3);

        private final int positionInList;
        Action(int pos){
            this.positionInList = pos;
        }

        public static Action getAction(int pos){
            switch(pos){
                case 0: return START;
                case 1: return STOP;
                case 2: return PAUSE;
            }
            return RESUME;
        }
    }

    private boolean checkValidity(){
        return wasEdited && met_dns1.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED
                && met_dns2.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED
                && met_name.getIndicatorState() == MaterialEditText.IndicatorState.CORRECT;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHandler.getAppTheme(this));
        setContentView(R.layout.tasker_configure_layout);
        LogFactory.writeMessage(this, LOG_TAG, "Activity created", getIntent());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        customPorts = PreferencesAccessor.areCustomPortsEnabled(this);

        ipv4Enabled = PreferencesAccessor.isIPv4Enabled(this);
        ipv6Enabled = !ipv4Enabled || PreferencesAccessor.isIPv6Enabled(this);
        settingV6 = !ipv4Enabled;

        ed_dns1 = findViewById(R.id.dns1);
        ed_dns2 = findViewById(R.id.dns2);
        ed_name = findViewById(R.id.name);
        met_dns1 = findViewById(R.id.met_dns1);
        met_dns2 = findViewById(R.id.met_dns2);
        met_name = findViewById(R.id.met_name);
        Spinner actionSpinner = findViewById(R.id.spinner);
        Helper.scrub(getIntent());
        final Bundle bundle = getIntent().getBundleExtra(Helper.EXTRA_BUNDLE);
        creatingShortcut = getIntent() != null && getIntent().getBooleanExtra("creatingShortcut", false);
        LogFactory.writeMessage(this, LOG_TAG, "Creating Shortcut: " + creatingShortcut);
        Helper.scrub(bundle);
        if (savedInstanceState == null) {
            if (Helper.isBundleValid(this, bundle)) {
                LogFactory.writeMessage(this, LOG_TAG, "Editing existing Tasker Configuration");
                if (bundle.containsKey(Helper.BUNDLE_EXTRA_DNS1))
                    dns1 = IPPortPair.wrap(bundle.getString(Helper.BUNDLE_EXTRA_DNS1));
                if (bundle.containsKey(Helper.BUNDLE_EXTRA_DNS2))
                    dns2 = IPPortPair.wrap(bundle.getString(Helper.BUNDLE_EXTRA_DNS2));
                if (bundle.containsKey(Helper.BUNDLE_EXTRA_DNS1V6))
                    dns1V6 = IPPortPair.wrap(bundle.getString(Helper.BUNDLE_EXTRA_DNS1V6));
                if (bundle.containsKey(Helper.BUNDLE_EXTRA_DNS2V6))
                    dns2V6 = IPPortPair.wrap(bundle.getString(Helper.BUNDLE_EXTRA_DNS2V6));
                if (getIntent().hasExtra(Helper.EXTRA_BLURB))
                    ed_name.setText(getIntent().getStringExtra(Helper.EXTRA_BLURB));

                if(bundle.containsKey(Helper.BUNDLE_EXTRA_PAUSE_DNS))
                    currentAction = Action.PAUSE;
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_RESUME_DNS))
                    currentAction = Action.RESUME;
                if(bundle.containsKey(Helper.BUNDLE_EXTRA_STOP_DNS))
                    currentAction = Action.STOP;
            }
        }
        ed_dns1.setText(settingV6 ? dns1V6.formatForTextfield(customPorts) : dns1.formatForTextfield(customPorts));
        ed_dns2.setText(settingV6 ? dns2V6.formatForTextfield(customPorts) : dns2.formatForTextfield(customPorts));
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
                        PreferencesAccessor.isLoopbackAllowed(ConfigureActivity.this), 53);
                if(pair == null || (pair.getPort() != 53 && !customPorts)){
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                }else{
                    wasEdited = true;
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if(settingV6)dns1V6 = pair;
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
                        PreferencesAccessor.isLoopbackAllowed(ConfigureActivity.this), 53);
                if(pair == null || (pair != IPPortPair.getEmptyPair() && pair.getPort() != 53 && !customPorts)){
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                }else{
                    wasEdited = true;
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if(settingV6)dns2V6 = pair;
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
                wasEdited = true;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        final Preferences preferences = Preferences.getInstance(this);
        if(!creatingShortcut && preferences.getBoolean( "setting_auto_wifi",false) || preferences.getBoolean( "setting_auto_mobile",false)
                || preferences.getBoolean( "setting_start_boot", false) || preferences.getBoolean( "setting_auto_disable",false)){
            new AlertDialog.Builder(this,ThemeHandler.getDialogTheme(this)).setTitle(R.string.warning).setMessage(creatingShortcut ? R.string.shortcut_conflict_text : R.string.tasker_automation_conflict_text).setCancelable(false).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    preferences.put("setting_auto_wifi", false);
                    preferences.put( "setting_auto_mobile", false);
                    preferences.put("setting_start_boot", false);
                    preferences.put( "setting_auto_disable", false);
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
                    currentAction = Action.getAction(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            actionSpinner.getBackground().setColorFilter(Color.parseColor("#ffffff"), PorterDuff.Mode.SRC_ATOP);
            actionSpinner.setSelection(currentAction.positionInList);
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
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setInputType();
    }

    public void openDefaultDNSDialog(View v){
        defaultDNSDialog = new DefaultDNSDialog(this, ThemeHandler.getDialogTheme(this), new DefaultDNSDialog.OnProviderSelectedListener() {
            @Override
            public void onProviderSelected(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6) {
                if(settingV6){
                    ed_dns1.setText(dns1V6.toString(customPorts));
                    ed_dns2.setText(dns2V6.toString(customPorts));
                    ConfigureActivity.this.dns1 = dns1;
                    ConfigureActivity.this.dns2 = dns2;
                }else{
                    ed_dns1.setText(dns1.toString(customPorts));
                    ed_dns2.setText(dns2.toString(customPorts));
                    ConfigureActivity.this.dns1V6 = dns1V6;
                    ConfigureActivity.this.dns2V6 = dns2V6;
                }
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
        getMenuInflater().inflate(ipv6Enabled && ipv4Enabled ? (settingV6 ? R.menu.tasker_menu_v4 : R.menu.tasker_menu_v6) : R.menu.tasker_menu_no_ipv6 ,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_switch_ip_version){
            settingV6 = !settingV6;
            invalidateOptionsMenu();
            setInputType();
            ed_dns1.setText(settingV6 ? dns1V6.formatForTextfield(customPorts) : dns1.formatForTextfield(customPorts));
            ed_dns2.setText(settingV6 ? dns2V6.formatForTextfield(customPorts) : dns2.formatForTextfield(customPorts));
            getSupportActionBar().setSubtitle(getString(R.string.subtitle_configuring).replace("[[x]]",settingV6 ? "Ipv6" : "Ipv4"));
        }else if(item.getItemId() == android.R.id.home){
            lastBackPress = System.currentTimeMillis();
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setInputType(){
        if(settingV6 || customPorts){
            ed_dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            ed_dns2.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        InputFilter filter;
        if(settingV6){
            filter = new InputCharacterFilter(customPorts ?
                    Pattern.compile("[0-9:a-f\\[\\]]") : Pattern.compile("[0-9:a-f]"));
        }else{
            filter = new InputCharacterFilter(customPorts ?
                    Pattern.compile("[0-9.:]") : Pattern.compile("[0-9.]"));
        }
        ed_dns1.setFilters(new InputFilter[]{filter});
        ed_dns2.setFilters(new InputFilter[]{filter});
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
                LogFactory.writeMessage(this, LOG_TAG, "Action is other than START");
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
            Util.createShortcut(this, createPortPair(), ed_name.getText().toString());
            setResult(RESULT_OK);
            LogFactory.writeMessage(this, LOG_TAG, "Shortcut added to Launcher");
            DatabaseHelper.getInstance(this).createShortcut(ed_name.getText().toString(),
                    ipv4Enabled ? dns1 : null,
                    !TextUtils.isEmpty(dns2.getAddress()) && ipv4Enabled ? dns2 : null,
                    ipv6Enabled ? dns1V6 : null,
                    !TextUtils.isEmpty(dns2V6.getAddress()) && ipv6Enabled ? dns2V6 : null);
        }
        super.finish();
    }

    private ArrayList<IPPortPair> createPortPair(){
        ArrayList<IPPortPair> list =  new ArrayList<>();
        list.add(dns1);list.add(dns2);list.add(dns1V6);list.add(dns2V6);
        IPPortPair pair;
        for(Iterator<IPPortPair> iterator = list.iterator(); iterator.hasNext();){
            pair = iterator.next();
            if(TextUtils.isEmpty(pair.getAddress()) || (!ipv4Enabled && !pair.isIpv6()) || (!ipv6Enabled && pair.isIpv6()))
                iterator.remove();
        }
        return list;
    }
}
