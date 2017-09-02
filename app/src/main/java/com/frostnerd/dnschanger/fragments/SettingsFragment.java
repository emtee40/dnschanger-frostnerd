package com.frostnerd.dnschanger.fragments;

import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.AppSelectionActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tasker.ConfigureActivity;
import com.frostnerd.utils.general.IntentUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.utils.preferences.Preferences;
import com.frostnerd.utils.preferences.searchablepreferences.SearchSettings;
import com.frostnerd.utils.preferences.searchablepreferences.v14.PreferenceSearcher;
import com.frostnerd.utils.preferences.searchablepreferences.v14.SearchablePreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SearchablePreference, SearchView.OnQueryTextListener{
    private boolean usageRevokeHidden = false;
    private PreferenceCategory automatingCategory, debugCategory;
    private Preference removeUsagePreference, sendDebugPreference;
    private DevicePolicyManager devicePolicyManager;
    //private ComponentName deviceAdmin;
    public static final int REQUEST_CODE_ENABLE_ADMIN = 1, REQUEST_CREATE_SHORTCUT = 2, REQUEST_EXCLUDE_APPS = 3;
    public final static String LOG_TAG = "[SettingsActivity]", ARGUMENT_SCROLL_TO_SETTING = "scroll_to_setting";
    public final static int USAGE_STATS_REQUEST = 13, CHOOSE_AUTOPAUSEAPPS_REQUEST = 14;
    private PreferenceScreen preferenceScreen;
    private PreferenceSearcher preferenceSearcher = new PreferenceSearcher(this);

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        if(getArguments() != null && getArguments().containsKey(ARGUMENT_SCROLL_TO_SETTING)){
            String key = getArguments().getString(ARGUMENT_SCROLL_TO_SETTING, null);
            if(!key.equals("") && key != null){
                scrollToPreference(key);
            }
        }
    }

    //Theme, getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        LogFactory.writeMessage(getActivity(), LOG_TAG, "Created Activity");
        LogFactory.writeMessage(getActivity(), LOG_TAG, "Added preferences from resources");
        devicePolicyManager = (DevicePolicyManager)getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        //deviceAdmin = new ComponentName(getActivity(), _AdminReceiver.class);
        preferenceScreen = (PreferenceScreen)findPreference("preferences");
        findPreference("setting_start_boot").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_show_notification").setOnPreferenceChangeListener(changeListener);
        findPreference("show_used_dns").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_auto_mobile").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_disable_netchange").setOnPreferenceChangeListener(changeListener);
        findPreference("notification_on_stop").setOnPreferenceChangeListener(changeListener);
        findPreference("shortcut_click_again_disable").setOnPreferenceChangeListener(changeListener);
        findPreference("shortcut_click_override_settings").setOnPreferenceChangeListener(changeListener);
        findPreference("pin_value").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getString(R.string.summary_pin_value).replace("[[x]]", ""+newValue));
                return true;
            }
        });
        findPreference("pin_value").setSummary(getString(R.string.summary_pin_value).replace("[[x]]", Preferences.getString(getActivity(), "pin_value", "1234")));
        if(API.isTaskerInstalled(getActivity()))findPreference("warn_automation_tasker").setSummary(R.string.summary_automation_warn);
        else ((PreferenceCategory)findPreference("automation")).removePreference(findPreference("warn_automation_tasker"));
        /*findPreference("auto_pause").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                if(!((Boolean) newValue))return true;
                if(!PermissionsUtil.hasUsageStatsPermission(getActivity())){
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "Access to usage stats is not yet granted. Showing dialog explaining why it's needed");
                    new AlertDialog.Builder(getActivity(),ThemeHandler.getDialogTheme(getActivity())).setTitle(R.string.information).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i;
                            LogFactory.writeMessage(getActivity(), LOG_TAG, "User clicked OK in Usage stats access dialog, opening Usage Stats settings",
                                    i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            startActivityForResult(i, USAGE_STATS_REQUEST);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(getActivity(), LOG_TAG, "User cancelled the request for access to usage stats dialog");
                            dialog.cancel();
                        }
                    }).setMessage(R.string.usage_stats_info_text).setCancelable(false).show();
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "Dialog is now being shown");
                    return false;
                }else return true;
            }
        });
        findPreference("autopause_appselect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                Set<String> apps = Preferences.getStringSet(getActivity(), "autopause_apps");
                startActivityForResult(new Intent(getActivity(), AppSelectionActivity.class).putExtra("apps", Collections.list(Collections.enumeration(apps))).
                        putExtra("infoText", getString(R.string.autopause_appselect_info_text)),CHOOSE_AUTOPAUSEAPPS_REQUEST);
                return true;
            }
        });*/
        automatingCategory = (PreferenceCategory)getPreferenceScreen().findPreference("automation");
        /*
        findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                replace("[[count]]", Preferences.getStringSet(getActivity(), "autopause_apps").size()+""));
        /*findPreference("export_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                importSettings = false;
                exportSettings = false;
                if(checkWriteReadPermission())exportSettingsAskShortcuts();
                else exportSettings = true;
                return true;
            }
        });
        findPreference("import_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                importSettings = false;
                exportSettings = false;
                if(checkWriteReadPermission())importSettings();
                else exportSettings = true;
                return true;
            }
        });*/
        /*if(devicePolicyManager.isAdminActive(deviceAdmin)) ((SwitchPreference) findPreference("device_admin")).setChecked(true);
        else{
            ((SwitchPreference) findPreference("device_admin")).setChecked(false);
            Preferences.put(getActivity(),"device_admin", false);
        }*/
        /*findPreference("device_admin").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                boolean value = (Boolean)newValue;
                if(value && !devicePolicyManager.isAdminActive(deviceAdmin)){
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "User wants app to function as DeviceAdmin but access isn't granted yet. Showing dialog explaining Device Admin");
                    new AlertDialog.Builder(getActivity(),ThemeHandler.getDialogTheme(getActivity())).setTitle(R.string.information).setMessage(R.string.set_device_admin_info).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    getString(R.string.device_admin_description));
                            LogFactory.writeMessage(getActivity(), LOG_TAG, "User clicked OK in dialog explaining DeviceAdmin. Going to settings", intent);
                            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(getActivity(), LOG_TAG, "User chose to cancel the dialog explaining DeviceAdmin");
                            dialog.cancel();
                        }
                    }).show();
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "Dialog is now being shown");
                    return false;
                }else if(!value){
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "User disabled Admin access. Removing as Deviceadmin");
                    Preferences.put(getActivity(),"device_admin", false);
                    devicePolicyManager.removeActiveAdmin(deviceAdmin);
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "App was removed as DeviceAdmin");
                }else{
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "User wants app to function as DeviceAdmin and Access was granted. Showing state as true.");
                    Preferences.put(getActivity(),"device_admin", true);
                }
                return true;
            }
        });*/
        sendDebugPreference = findPreference("send_debug");
        debugCategory = (PreferenceCategory)findPreference("debug_category");
        if(!Preferences.getBoolean(getActivity(), "debug",false))debugCategory.removePreference(sendDebugPreference);
        findPreference("debug").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preferences.put(getActivity(),preference.getKey(),newValue);
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                boolean val = (Boolean)newValue;
                if(!val){
                    debugCategory.removePreference(sendDebugPreference);
                    LogFactory.disable();
                    return true;
                }
                new AlertDialog.Builder(getActivity(),ThemeHandler.getDialogTheme(getActivity())).setTitle(R.string.warning).setMessage(R.string.debug_dialog_info_text).setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((SwitchPreference)findPreference("debug")).setChecked(true);
                                Preferences.put(getActivity(), "debug",true);
                                LogFactory.enable(getActivity());
                                debugCategory.addPreference(sendDebugPreference);
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
                return false;
            }
        });
        sendDebugPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                File zip = LogFactory.zipLogFiles(getActivity());
                if(zip == null)return true;
                Uri zipURI = FileProvider.getUriForFile(getActivity(),"com.frostnerd.dnschanger",zip);
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                for(ResolveInfo resolveInfo: getActivity().getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)){
                    getActivity().grantUriPermission(resolveInfo.activityInfo.packageName,zipURI, Intent.FLAG_GRANT_READ_URI_PERMISSION );
                }
                emailIntent.putExtra(Intent.EXTRA_STREAM, zipURI);
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Now showing chooser for sending debug logs to dev", emailIntent);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        findPreference("create_shortcut").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                Intent i;
                LogFactory.writeMessage(getActivity(), LOG_TAG, "User wants to create a shortcut",
                        i = new Intent(getActivity(), ConfigureActivity.class).putExtra("creatingShortcut", true));
                startActivityForResult(i,REQUEST_CREATE_SHORTCUT);
                return true;
            }
        });
        findPreference("exclude_apps").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Set<String> apps = Preferences.getStringSet(getActivity(), "excluded_apps");
                startActivityForResult(new Intent(getActivity(), AppSelectionActivity.class).putExtra("apps", Collections.list(Collections.enumeration(apps))).
                        putExtra("infoTextWhitelist", getString(R.string.excluded_apps_info_text_whitelist)).putExtra("infoTextBlacklist", getString(R.string.excluded_apps_info_text_blacklist))
                        .putExtra("whitelist", Preferences.getBoolean(getActivity(), "excluded_whitelist",false)).putExtra("onlyInternet",true),REQUEST_EXCLUDE_APPS);
                return true;
            }
        });
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)((PreferenceCategory)findPreference("general_category")).removePreference(findPreference("exclude_apps"));
        findPreference("reset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                LogFactory.writeMessage(getActivity(), LOG_TAG, preference.getKey() + " clicked");
                new AlertDialog.Builder(getActivity(),ThemeHandler.getDialogTheme(getActivity())).setTitle(R.string.warning).setMessage(R.string.reset_warning_text).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogFactory.writeMessage(getActivity(), LOG_TAG, "Resetting..");
                        Preferences.getDefaultPreferences(getActivity()).edit().clear().commit();
                        Preferences.flushBuffer();
                        API.deleteDatabase(getActivity());
                        LogFactory.writeMessage(getActivity(), LOG_TAG, "Reset finished.");
                        getActivity().finish();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
                return true;
            }
        });
        findPreference("theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String val = (String)newValue;
                int theme = val.equalsIgnoreCase("1") ? R.style.AppTheme : (val.equalsIgnoreCase("2") ? R.style.AppTheme_Mono : R.style.AppTheme_Dark),
                        dialogTheme = val.equalsIgnoreCase("1") ? R.style.DialogTheme : (val.equalsIgnoreCase("2") ? R.style.DialogTheme_Mono : R.style.DialogTheme_Dark);
                ThemeHandler.updateAppTheme(getActivity(), theme);
                ThemeHandler.updateDialogTheme(getActivity(), dialogTheme);
                IntentUtil.restartActivity(getActivity());
                return true;
            }
        });
        findPreference("pin_app_shortcut").setOnPreferenceChangeListener(changeListener);
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1){
            ((PreferenceCategory)findPreference("general_category")).removePreference(findPreference("setting_app_shortcuts_enabled"));
            ((PreferenceCategory)findPreference("pin_category")).removePreference(findPreference("pin_app_shortcut"));
        }else findPreference("setting_app_shortcuts_enabled").setOnPreferenceChangeListener(changeListener);
        ((ListPreference)findPreference("theme")).setDefaultValue(0);
        LogFactory.writeMessage(getActivity(), LOG_TAG, "Done with onCreate");
        final CheckBoxPreference v4Enabled = (CheckBoxPreference)  findPreference("setting_ipv4_enabled"),
                v6Enabled = (CheckBoxPreference)findPreference("setting_ipv6_enabled");
        v4Enabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object newValue) {
                final boolean val = (boolean)newValue;
                Preferences.put(getActivity(),preference.getKey(),newValue);
                if(!val)new AlertDialog.Builder(getActivity(), ThemeHandler.getDialogTheme(getActivity())).setNegativeButton(R.string.cancel, null).
                        setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                v6Enabled.setEnabled(val);
                                if(API.isServiceRunning(getActivity()))getActivity().startService(new Intent(getActivity(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                                v4Enabled.setChecked(val);
                            }
                        }).setTitle(R.string.warning).setMessage(R.string.warning_disabling_v4).show();
                else if(API.isServiceRunning(getActivity())){
                    getActivity().startService(new Intent(getActivity(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                }
                v6Enabled.setEnabled(val);
                return val;
            }
        });
        v4Enabled.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                return true;
            }
        });
        v6Enabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean val = (boolean)newValue;
                v4Enabled.setEnabled(val);
                Preferences.put(getActivity(),preference.getKey(),newValue);
                if(API.isServiceRunning(getActivity()))getActivity().startService(new Intent(getActivity(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                return true;
            }
        });
        v4Enabled.setEnabled(v6Enabled.isEnabled());
        v6Enabled.setEnabled(v4Enabled.isEnabled());
        if(Preferences.getBoolean(getActivity(), "excluded_whitelist", false)){
            findPreference("excluded_whitelist").setSummary(R.string.excluded_apps_info_text_whitelist);
        }
        findPreference("excluded_whitelist").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean newValue = (boolean)o;
                Preferences.put(getActivity(), "app_whitelist_configured", true);
                preference.setSummary(newValue ? R.string.excluded_apps_info_text_whitelist : R.string.excluded_apps_info_text_blacklist);
                Set<String> selected = Preferences.getStringSet(getActivity(), "excluded_apps");
                Set<String> flipped = new HashSet<>();
                List<ApplicationInfo> packages = getActivity().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo packageInfo : packages) {
                    if(selected.contains(packageInfo.packageName))continue;
                    flipped.add(packageInfo.packageName);
                }
                Preferences.put(getActivity(), "excluded_apps", flipped);
                return true;
            }
        });
        findPreference("setting_pin_enabled").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if((boolean)newValue){
                    if(Preferences.getString(getActivity(), "pin_value", "1234").equals("1234"))getPreferenceManager().showDialog(findPreference("pin_value"));
                    if (!((CheckBoxPreference)findPreference("pin_app")).isChecked())((CheckBoxPreference)findPreference("pin_app")).setChecked(true);
                }
                return true;
            }
        });
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Preference pref = findPreference("setting_show_notification");
            ((CheckBoxPreference)pref).setChecked(true);
            pref.setSummary(pref.getSummary() + "\n" + getString(R.string.no_disable_android_o));
            pref.setEnabled(false);
            findPreference("show_used_dns").setDependency("");
        }
    }

    private Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            LogFactory.writeMessage(getActivity(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                    newValue + ", Type: " + Preferences.getType(newValue));
            Preferences.put(getActivity(),preference.getKey(),newValue);
            String key = preference.getKey();
            if((key.equalsIgnoreCase("setting_show_notification") || key.equalsIgnoreCase("show_used_dns") ||
                    key.equalsIgnoreCase("auto_pause")) && API.isServiceRunning(getActivity())){
                Intent i;
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Updating DNSVPNService, as a relevant setting " +
                        "(notification/autopause) changed", i = new Intent(getActivity(), DNSVpnService.class));
                getActivity().startService(i);
            }
            if(key.equals("pin_app_shortcut") || key.equals("setting_app_shortcuts_enabled"))API.updateAppShortcuts(getActivity());
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        LogFactory.writeMessage(getActivity(), LOG_TAG, "Resuming Activity");
//        if(devicePolicyManager.isAdminActive(deviceAdmin)){
//            ((SwitchPreference)findPreference("device_admin")).setChecked(true);
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogFactory.writeMessage(getActivity(), LOG_TAG, "Received onActivityResult", data);
        if(requestCode == USAGE_STATS_REQUEST){
            LogFactory.writeMessage(getActivity(), LOG_TAG, "Got answer to the Usage Stats request");
            if(PermissionsUtil.hasUsageStatsPermission(getActivity())){
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Permission to usage stats was granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(true);
                Preferences.put(getActivity(), "auto_pause",true);
                if(usageRevokeHidden){
                    automatingCategory.addPreference(removeUsagePreference);
                    usageRevokeHidden = false;
                }
            }else{
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Permission to usage stats wasn't granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(false);
                Preferences.put(getActivity(), "auto_pause",false);
                if(!usageRevokeHidden){
                    LogFactory.writeMessage(getActivity(), LOG_TAG, "Access was previously granted, hiding 'Revoke access' preference");
                    automatingCategory.removePreference(removeUsagePreference);
                    usageRevokeHidden = true;
                }
            }
        }else if(requestCode == CHOOSE_AUTOPAUSEAPPS_REQUEST && resultCode == AppCompatActivity.RESULT_OK){
            LogFactory.writeMessage(getActivity(), LOG_TAG, "User returned from configuring autopause apps");
            ArrayList<String> apps = data.getStringArrayListExtra("apps");
            findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                    replace("[[count]]", ""+ apps.size()));
            if(apps.size() != getResources().getStringArray(R.array.default_blacklist).length)Preferences.put(getActivity(), "app_whitelist_configured", true);
            Preferences.put(getActivity(), "autopause_apps", new HashSet<String>(apps));
            Preferences.put(getActivity(), "autopause_apps_count", apps.size());
            if(API.isServiceRunning(getActivity())){
                Intent i;
                LogFactory.writeMessage(getActivity(), LOG_TAG, "Restarting DNSVPNService because the autopause apps changed",
                        i = new Intent(getActivity(), DNSVpnService.class));
                getActivity().startService(i);
            }
        /*}else if(requestCode == REQUEST_CODE_ENABLE_ADMIN && resultCode == AppCompatActivity.RESULT_OK && devicePolicyManager.isAdminActive(deviceAdmin)){
            LogFactory.writeMessage(getActivity(), LOG_TAG, "Deviceadmin was activated");
            ((SwitchPreference)findPreference("device_admin")).setChecked(true);*/
        }else if(requestCode == REQUEST_CREATE_SHORTCUT && resultCode == AppCompatActivity.RESULT_OK){
            final Snackbar snackbar = Snackbar.make(getListView(), R.string.shortcut_created, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(snackbar != null)snackbar.dismiss();
                    Utils.goToLauncher(getActivity());
                }
            });
            snackbar.show();
        }else if(requestCode == REQUEST_EXCLUDE_APPS && resultCode == AppCompatActivity.RESULT_OK){
            ArrayList<String> apps = data.getStringArrayListExtra("apps");
            Preferences.put(getActivity(), "excluded_apps", new HashSet<String>(apps));
            Preferences.put(getActivity(), "excluded_whitelist", data.getBooleanExtra("whitelist",false));
            if(API.isServiceRunning(getActivity())){
                getActivity().startService(new Intent(getActivity(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true).
                        putExtra(VPNServiceArgument.FLAG_DONT_UPDATE_DNS.getArgument(),true));
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);

        SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
    }

    private Pattern emptySearchPattern = Pattern.compile("[\\s]*?");
    @Override
    public boolean preferenceMatches(Preference preference, String search) {
        if(search == null || search.equals("") || emptySearchPattern.matcher(search).matches())return true;
        Pattern pattern = Pattern.compile("(?i).*?" + search + ".*");
        if(preference.getTitle() == null && preference.getSummary() != null){
            return pattern.matcher(preference.getSummary()).matches();
        }else if (preference.getSummary() == null && preference.getTitle() != null) {
            return pattern.matcher(preference.getTitle()).matches();
        } else
            return preference.getSummary() != null && pattern.matcher(preference.getTitle() + "" + preference.getSummary()).matches();
    }

    @Override
    public SearchSettings getSearchOptions() {
        return new SearchSettings.Builder().hideCategoriesWithNoChildren(true).matchCategories(false).build();
    }

    @Override
    public android.support.v7.preference.PreferenceGroup getTopLevelPreferenceGroup() {
        return getPreferenceScreen();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        preferenceSearcher.search(newText);
        return true;
    }
}
