package com.frostnerd.dnschanger.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.OrientationHelper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.DNSEntry;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class MainFragment extends Fragment {
    private Button startStopButton;
    private boolean vpnRunning, wasStartedWithTasker = false;
    private MaterialEditText met_dns1, met_dns2;
    public EditText dns1, dns2;
    private boolean doStopVPN = true;
    private static final String LOG_TAG = "[MainActivity]";
    private TextView connectionText;
    private ImageView connectionImage;
    private View running_indicator;
    private View wrapper;
    public boolean settingV6 = false;
    private final int REQUEST_SETTINGS = 13;
    private AlertDialog dialog2;
    private BroadcastReceiver serviceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogFactory.writeMessage(getContext(), LOG_TAG, "Received ServiceState Answer", intent);
            vpnRunning = intent.getBooleanExtra("vpn_running",false);
            wasStartedWithTasker = intent.getBooleanExtra("started_with_tasker", false);
            setIndicatorState(intent.getBooleanExtra("vpn_running",false));
        }
    };
    private View contentView;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sp, String s) {
            if(s.equals("everything_disabled")){
                boolean value = Preferences.getBoolean(getContext(), "everything_disabled", false);
                startStopButton.setEnabled(!value);
                startStopButton.setClickable(!value);
                startStopButton.setAlpha(value ? 0.50f : 1f);
                if(value)connectionText.setText(R.string.info_functionality_disabled);
                else setIndicatorState(vpnRunning);
            }
        }
    };

    private void setIndicatorState(boolean vpnRunning) {
        LogFactory.writeMessage(getContext(), LOG_TAG, "Changing IndicatorState to " + vpnRunning);
        if (vpnRunning) {
            int color = Color.parseColor("#42A5F5");
            connectionText.setText(R.string.running);
            if(connectionImage != null)connectionImage.setImageResource(R.drawable.ic_thumb_up);
            startStopButton.setText(R.string.stop);
            running_indicator.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getContext().getTheme();
            theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true);
            connectionText.setText(R.string.not_running);
            if(connectionImage != null)connectionImage.setImageResource(R.drawable.ic_thumb_down);
            startStopButton.setText(R.string.start);
            running_indicator.setBackgroundColor(typedValue.data);
        }
        LogFactory.writeMessage(getContext(), LOG_TAG, "IndictorState set");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.fragment_main, container, false);
        return contentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        settingV6 = !API.isIPv4Enabled(getContext()) || (API.isIPv6Enabled(getContext()) && settingV6);
        setHasOptionsMenu(true);
        boolean vertical = getResources().getConfiguration().orientation == OrientationHelper.VERTICAL;
        LogFactory.writeMessage(getContext(), LOG_TAG, "Created Activity", API.getActivity(this).getIntent());
        LogFactory.writeMessage(getContext(), LOG_TAG, "Setting ContentView");
        met_dns1 = (MaterialEditText) findViewById(R.id.met_dns1);
        met_dns2 = (MaterialEditText) findViewById(R.id.met_dns2);
        dns1 = (EditText) findViewById(R.id.dns1);
        dns2 = (EditText) findViewById(R.id.dns2);
        connectionImage = vertical ? null : (ImageView)findViewById(R.id.connection_status_image);
        connectionText = (TextView)findViewById(R.id.connection_status_text);
        wrapper = findViewById(R.id.activity_main);
        running_indicator = findViewById(R.id.running_indicator);
        startStopButton = (Button) findViewById(R.id.startStopButton);

        dns1.setText(Preferences.getString(getContext(),settingV6 ? "dns1-v6" : "dns1", settingV6 ? "2001:4860:4860::8888" : "8.8.8.8"));
        dns2.setText(Preferences.getString(getContext(),settingV6 ? "dns1-v6" : "dns1", settingV6 ? "2001:4860:4860::8844" : "8.8.4.4"));
        if(settingV6){
            dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            dns2.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getContext() == null)return;
                final Intent i = VpnService.prepare(getContext());
                LogFactory.writeMessage(getContext(), LOG_TAG, "Startbutton clicked. Configuring VPN if needed");
                if (i != null){
                    LogFactory.writeMessage(getContext(), LOG_TAG, "VPN isn't prepared yet. Showing dialog explaining the VPN");
                    dialog2 = new AlertDialog.Builder(getContext(),ThemeHandler.getDialogTheme(getContext())).setTitle(R.string.information).setMessage(R.string.vpn_explain)
                            .setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                    LogFactory.writeMessage(getContext(), LOG_TAG, "Requesting VPN access", i);
                                    startActivityForResult(i, 0);
                                }
                            }).show();
                    LogFactory.writeMessage(getContext(), LOG_TAG, "Dialog is now being shown");
                }else{
                    LogFactory.writeMessage(getContext(), LOG_TAG, "VPNService is already configured");
                    onActivityResult(0, Activity.RESULT_OK, null);
                }
            }
        });
        dns1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(before != count){
                    if(vpnRunning && doStopVPN && !wasStartedWithTasker)stopVpn();
                    if (!NetworkUtil.isAssignableAddress(s.toString(),settingV6,false)) {
                        met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                    } else {
                        met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                        Preferences.put(getContext(), settingV6 ? "dns1-v6" :"dns1", s.toString());
                        setEditTextLabel();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        dns2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(before != count){
                    if(vpnRunning && doStopVPN && !wasStartedWithTasker)stopVpn();
                    if (!NetworkUtil.isAssignableAddress(s.toString(),settingV6, true)) {
                        met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                    } else {
                        met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                        Preferences.put(getContext(), settingV6 ? "dns2-v6" : "dns2", s.toString());
                        setEditTextLabel();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        setEditTextLabel();
        LogFactory.writeMessage(getContext(), LOG_TAG, "Done with OnCreate");
    }

    private void setEditTextLabel(){
        String label1 = "DNS 1", label2 = "DNS 2";
        for(DNSEntry entry: API.getDBHelper(getContext()).getDNSEntries()){
            if(entry.hasIP(dns1.getText().toString()))label1 = "DNS 1 (" + entry.getName() + ")";
            if(entry.hasIP(dns2.getText().toString()))label2 = "DNS 2 (" + entry.getName() + ")";
        }
        met_dns1.setLabelText(label1);
        met_dns2.setLabelText(label2);
    }

    private View findViewById(@IdRes int id){
        return contentView.findViewById(id);
    }

    @Override
    public void onResume() {
        super.onResume();
        Preferences.getDefaultPreferences(getContext()).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        settingV6 = !API.isIPv4Enabled(getContext()) || (API.isIPv6Enabled(getContext()) && settingV6);
        LogFactory.writeMessage(getContext(), LOG_TAG, "Got OnResume");
        LogFactory.writeMessage(getContext(), LOG_TAG, "Sending ServiceStateRequest as broadcast");
        vpnRunning = API.isServiceRunning(getContext());
        if(Preferences.getBoolean(getContext(), "everything_disabled", false)){
            startStopButton.setEnabled(false);
            startStopButton.setClickable(false);
            startStopButton.setAlpha(0.50f);
            connectionText.setText(R.string.info_functionality_disabled);
        }else{
            startStopButton.setEnabled(true);
            startStopButton.setClickable(true);
            startStopButton.setAlpha(1f);
            setIndicatorState(vpnRunning);
        }
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(serviceStateReceiver, new IntentFilter(API.BROADCAST_SERVICE_STATUS_CHANGE));
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(API.BROADCAST_SERVICE_STATE_REQUEST));
        doStopVPN = false;
        if(!settingV6){
            dns1.setText(Preferences.getString(getContext(), "dns1", "8.8.8.8"));
            dns2.setText(Preferences.getString(getContext(), "dns2", "8.8.4.4"));
        }else{
            dns1.setText(Preferences.getString(getContext(), "dns1-v6", "2001:4860:4860::8888"));
            dns2.setText(Preferences.getString(getContext(), "dns2-v6", "2001:4860:4860::8844"));
            dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            dns2.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        ((AppCompatActivity)getContext()).getSupportActionBar().setSubtitle(getString(R.string.subtitle_configuring).replace("[[x]]",settingV6 ? "Ipv6" : "Ipv4"));
        API.getActivity(this).invalidateOptionsMenu();
        doStopVPN = true;
        API.getActivity(this).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        LogFactory.writeMessage(getContext(), LOG_TAG, "Got OnPause");
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(serviceStateReceiver);
        Preferences.getDefaultPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogFactory.writeMessage(getContext(), LOG_TAG, "Got OnActivityResult" ,data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            if (!vpnRunning){
                if(!Preferences.getBoolean(getContext(), "44explained", false) && Build.VERSION.SDK_INT == 19){
                    LogFactory.writeMessage(getContext(), LOG_TAG, "Opening Dialog explaining that this might not work on Android 4.4");
                    new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext())).setTitle(R.string.warning).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            startVpn();
                        }
                    }).setMessage(R.string.android4_4_warning).show();
                    LogFactory.writeMessage(getContext(), LOG_TAG, "Dialog is now being shown");
                }else{
                    startVpn();
                }
                Preferences.getBoolean(getContext(), "44explained", true);
            }else{
                if(wasStartedWithTasker){
                    LogFactory.writeMessage(getContext(), LOG_TAG, "Opening dialog which warns that the app was started using Tasker");
                    new AlertDialog.Builder(getContext(),ThemeHandler.getDialogTheme(getContext())).setTitle(R.string.warning).setMessage(R.string.warning_started_using_tasker). setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(getContext(), LOG_TAG, "User clicked OK in the dialog warning about Tasker");
                            stopVpn();
                            dialog.cancel();
                        }
                    }).setCancelable(false).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            LogFactory.writeMessage(getContext(), LOG_TAG, "User cancelled stopping DNSChanger as it was started using tasker");
                        }
                    }).show();
                    LogFactory.writeMessage(getContext(), LOG_TAG, "Dialog is now being shown");
                }else stopVpn();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startVpn() {
        Intent i;
        LogFactory.writeMessage(getContext(), LOG_TAG, "Starting VPN",
                i = DNSVpnService.getStartVPNIntent(getContext()));
        wasStartedWithTasker = false;
        API.startService(getContext(), i);
        vpnRunning = true;
        setIndicatorState(true);
    }

    private void stopVpn() {
        Intent i;
        LogFactory.writeMessage(getContext(), LOG_TAG, "Stopping VPN",
                i = DNSVpnService.getDestroyIntent(getContext()));
        getContext().startService(i);
        vpnRunning = false;
        setIndicatorState(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(API.isIPv6Enabled(getContext()) ? (API.isIPv4Enabled(getContext()) ? ((settingV6 ? R.menu.menu_main_v6 : R.menu.menu_main)) : R.menu.menu_main_no_ipv6) : R.menu.menu_main_no_ipv6,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_switch_ip_version){
            doStopVPN = false;
            settingV6 = !settingV6;
            API.getActivity(this).invalidateOptionsMenu();
            dns1.setText(Preferences.getString(getContext(),settingV6 ? "dns1-v6" : "dns1", settingV6 ? "2001:4860:4860::8888" : "8.8.8.8"));
            dns2.setText(Preferences.getString(getContext(),settingV6 ? "dns2-v6" : "dns2", settingV6 ? "2001:4860:4860::8844" : "8.8.4.4"));
            dns1.setInputType(InputType.TYPE_CLASS_TEXT);
            dns2.setInputType(InputType.TYPE_CLASS_TEXT);
            ((AppCompatActivity)getContext()).getSupportActionBar().setSubtitle(getString(R.string.subtitle_configuring).replace("[[x]]",settingV6 ? "Ipv6" : "Ipv4"));
            doStopVPN = true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context == null ? MainActivity.currentContext : context;
    }
}
