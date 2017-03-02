package com.frostnerd.dnschanger.tasker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.frostnerd.dnschanger.MainActivity;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class ConfigureActivity extends AppCompatActivity {
    private MaterialEditText met_dns1, met_dns2;
    private EditText ed_dns1, ed_dns2;
    private boolean cancelled = false;
    private AlertDialog defaultDnsDialog;
    private String dns1 = "8.8.8.8", dns2 = "8.8.4.4", dns1V6 ="2001:4860:4860::8888", dns2V6 = "2001:4860:4860::8844";
    private static final HashMap<String, List<String>> defaultDNS = new HashMap<>();
    private static final HashMap<String, List<String>> defaultDNS_V6 = new HashMap<>();
    private static final List<String> defaultDNSKeys, DefaultDNSKeys_V6;
    private boolean settingV6 = false;

    static {
        defaultDNS.put("Google DNS", Arrays.asList("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"));
        defaultDNS.put("OpenDNS", Arrays.asList("208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2"));
        defaultDNS.put("Level3", Arrays.asList("209.244.0.3", "209.244.0.4"));
        defaultDNS.put("FreeDNS", Arrays.asList("37.235.1.174", "37.235.1.177"));
        defaultDNS.put("Yandex DNS", Arrays.asList("77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff"));
        defaultDNS.put("Verisign", Arrays.asList("64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2"));
        defaultDNS.put("Alternate DNS", Arrays.asList("198.101.242.72", "23.253.163.53"));

        defaultDNS_V6.put("Google DNS", Arrays.asList("2001:4860:4860::8888", "2001:4860:4860::8844"));
        defaultDNS_V6.put("OpenDNS", Arrays.asList("2620:0:ccc::2", "2620:0:ccd::2"));
        defaultDNS_V6.put("Yandex DNS", Arrays.asList("2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff"));
        defaultDNS_V6.put("Verisign", Arrays.asList("2620:74:1b::1:1", "2620:74:1c::2:2"));
        defaultDNSKeys = new ArrayList<>(defaultDNS.keySet());
        DefaultDNSKeys_V6 = new ArrayList<>(defaultDNS_V6.keySet());
    }

    private boolean checkValidity(){
        return Utils.isIP(dns1,false) && Utils.isIP(dns2,false) && Utils.isIP(dns1V6,true) && Utils.isIP(dns2V6,true);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasker_configure_layout);
        ed_dns1 = (EditText)findViewById(R.id.dns1);
        ed_dns2 = (EditText)findViewById(R.id.dns2);
        met_dns1 = (MaterialEditText) findViewById(R.id.met_dns1);
        met_dns2 = (MaterialEditText)findViewById(R.id.met_dns2);
        Helper.scrub(getIntent());
        final Bundle bundle = getIntent().getBundleExtra(Helper.EXTRA_BUNDLE);
        Helper.scrub(bundle);
        if(savedInstanceState == null){
            if(Helper.isBundleValid(bundle)){
                if(bundle.containsKey("dns1"))dns1 = bundle.getString("dns1");
                if(bundle.containsKey("dns2"))dns2 = bundle.getString("dns2");
                if(bundle.containsKey("dns1-v6"))dns1V6 = bundle.getString("dns1-v6");
                if(bundle.containsKey("dns2-v6"))dns2V6 = bundle.getString("dns2-v6");
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
                if (!Utils.isIP(s.toString(),settingV6)) {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
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
                if (!Utils.isIP(s.toString(),settingV6)) {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    if(settingV6)dns2V6 = s.toString();
                    else dns2 = s.toString();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void openDefaultDNSDialog(View v){
        defaultDnsDialog.show();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        View layout = getLayoutInflater().inflate(R.layout.dialog_default_dns, null, false);
        final ListView list = (ListView) layout.findViewById(R.id.defaultDnsDialogList);
        list.setAdapter(new DefaultDNSAdapter());
        list.setDividerHeight(0);
        defaultDnsDialog = new AlertDialog.Builder(this).setView(layout).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).setTitle(R.string.default_dns_title).create();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                defaultDnsDialog.cancel();
                List<String> ips = settingV6 ? defaultDNS_V6.get(DefaultDNSKeys_V6.get(position)) : defaultDNS.get(defaultDNSKeys.get(position));
                ed_dns1.setText(ips.get(0));
                ed_dns2.setText(ips.get(1));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(defaultDnsDialog != null)defaultDnsDialog.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(settingV6 ? R.menu.tasker_menu_v4 : R.menu.tasker_menu_v6 ,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_switch_ip_version){
            settingV6 = !settingV6;
            invalidateOptionsMenu();
            ed_dns1.setText(Preferences.getString(this,settingV6 ? "dns1-v6" : "dns1", settingV6 ? "2001:4860:4860::8888" : "8.8.8.8"));
            ed_dns2.setText(Preferences.getString(this,settingV6 ? "dns2-v6" : "dns2", settingV6 ? "2001:4860:4860::8844" : "8.8.4.4"));
            ed_dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            ed_dns2.setInputType(InputType.TYPE_CLASS_TEXT);
            getSupportActionBar().setSubtitle(getString(R.string.subtitle_configuring).replace("[[x]]",settingV6 ? "Ipv6" : "Ipv4"));
        }else if(item.getItemId() == R.id.menu_cancel){
            cancelled = true;
            finish();
        }else if(item.getItemId() == R.id.menu_done){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        if(!cancelled){
            final Intent resultIntent = new Intent();
            final Bundle resultBundle = Helper.createBundle(getApplicationContext(), dns1, dns2, dns1V6, dns2V6);
            resultIntent.putExtra(Helper.EXTRA_BUNDLE, resultBundle);

            resultIntent.putExtra(Helper.EXTRA_BLURB, "DNS Blurb");

            setResult(RESULT_OK, resultIntent);
        }
        super.finish();
    }

    private class DefaultDNSAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return settingV6 ? defaultDNS_V6.size() : defaultDNS.size();
        }

        @Override
        public Object getItem(int position) {
            return settingV6 ? defaultDNS_V6.get(position) : defaultDNS.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = getLayoutInflater().inflate(R.layout.item_default_dns, parent, false);
            ((TextView) v.findViewById(R.id.text)).setText(settingV6 ? DefaultDNSKeys_V6.get(position) : defaultDNSKeys.get(position));
            v.setTag(getItem(position));
            return v;
        }
    }

}
