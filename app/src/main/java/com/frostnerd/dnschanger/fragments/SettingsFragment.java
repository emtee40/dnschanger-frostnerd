package com.frostnerd.dnschanger.fragments;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.AdvancedSettingsActivity;
import com.frostnerd.dnschanger.activities.AppSelectionActivity;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.receivers.AdminReceiver;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tasker.ConfigureActivity;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.general.IntentUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.dnschanger.util.Preferences;
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
public class SettingsFragment extends PreferenceFragmentCompat implements SearchablePreference, SearchView.OnQueryTextListener {
    private boolean usageRevokeHidden = false, awaitingPinChange = false;
    private PreferenceCategory automatingCategory, debugCategory;
    private Preference removeUsagePreference, sendDebugPreference;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1, REQUEST_CREATE_SHORTCUT = 2,
            REQUEST_EXCLUDE_APPS = 3, REQUEST_FINGERPRINT_PERMISSION = 4, REQUEST_ADVANCED_SETTINGS = 5;
    private final static String LOG_TAG = "[SettingsActivity]";
    public static final String ARGUMENT_SCROLL_TO_SETTING = "scroll_to_setting";
    private final static int USAGE_STATS_REQUEST = 13, CHOOSE_AUTOPAUSEAPPS_REQUEST = 14;
    private final PreferenceSearcher preferenceSearcher = new PreferenceSearcher(this);
    private final Handler handler = new Handler();
    private Snackbar ipv6EnableQuestionSnackbar;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        if (getArguments() != null && getArguments().containsKey(ARGUMENT_SCROLL_TO_SETTING)) {
            String key = getArguments().getString(ARGUMENT_SCROLL_TO_SETTING, null);
            if (key != null && !key.equals("")) {
                scrollToPreference(key);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(requireContext(), LOG_TAG, "Created Activity");
        LogFactory.writeMessage(requireContext(), LOG_TAG, "Added preferences from resources");
        devicePolicyManager = (DevicePolicyManager) requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(requireContext(), AdminReceiver.class);
        findPreference("setting_start_boot").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_show_notification").setOnPreferenceChangeListener(changeListener);
        findPreference("show_used_dns").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_auto_mobile").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_disable_netchange").setOnPreferenceChangeListener(changeListener);
        findPreference("notification_on_stop").setOnPreferenceChangeListener(changeListener);
        findPreference("shortcut_click_again_disable").setOnPreferenceChangeListener(changeListener);
        
        final Preferences preferences = Preferences.getInstance(requireContext());
        
        if (Util.isTaskerInstalled(requireContext()))
            findPreference("warn_automation_tasker").setSummary(R.string.summary_automation_warn);
        else
            ((PreferenceCategory) findPreference("automation")).removePreference(findPreference("warn_automation_tasker"));
        automatingCategory = (PreferenceCategory) getPreferenceScreen().findPreference("automation");
        if (devicePolicyManager.isAdminActive(deviceAdmin))
            ((SwitchPreference) findPreference("device_admin")).setChecked(true);
        else {
            ((SwitchPreference) findPreference("device_admin")).setChecked(false);
            preferences.put("device_admin", false);
        }
        findPreference("device_admin").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.guessType(newValue));
                boolean value = (Boolean) newValue;
                if (value && !devicePolicyManager.isAdminActive(deviceAdmin)) {
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "User wants app to function as DeviceAdmin but access isn't granted yet. Showing dialog explaining Device Admin");
                    new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext())).setTitle(R.string.information).setMessage(R.string.set_device_admin_info).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    getString(R.string.device_admin_description));
                            LogFactory.writeMessage(requireContext(), LOG_TAG, "User clicked OK in dialog explaining DeviceAdmin. Going to settings", intent);
                            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(requireContext(), LOG_TAG, "User chose to cancel the dialog explaining DeviceAdmin");
                            dialog.cancel();
                        }
                    }).show();
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "Dialog is now being shown");
                    return false;
                } else if (!value) {
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "User disabled Admin access. Removing as Deviceadmin");
                   preferences.put("device_admin", false);
                    devicePolicyManager.removeActiveAdmin(deviceAdmin);
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "App was removed as DeviceAdmin");
                } else {
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "User wants app to function as DeviceAdmin and Access was granted. Showing state as true.");
                   preferences.put("device_admin", true);
                }
                return true;
            }
        });
        sendDebugPreference = findPreference("send_debug");
        debugCategory = (PreferenceCategory) findPreference("debug_category");
        if (!PreferencesAccessor.isDebugEnabled(requireContext()))
            debugCategory.removePreference(sendDebugPreference);
        findPreference("debug").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
               preferences.put(preference.getKey(), newValue);
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.guessType(newValue));
                boolean val = (Boolean) newValue;
                if (!val) {
                    debugCategory.removePreference(sendDebugPreference);
                    LogFactory.disable();
                    return true;
                }
                new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext())).setTitle(R.string.warning).setMessage(R.string.debug_dialog_info_text).setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((SwitchPreference) findPreference("debug")).setChecked(true);
                               preferences.put("debug", true);
                                LogFactory.enable(requireContext());
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
                LogFactory.writeMessage(requireContext(), LOG_TAG, preference.getKey() + " clicked");
                File zip = LogFactory.zipLogFiles(requireContext());
                if (zip == null) return true;
                Uri zipURI = FileProvider.getUriForFile(requireContext(), "com.frostnerd.dnschanger", zip);
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "support@frostnerd.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + BuildConfig.VERSION_NAME);
                emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.mail_debug_text));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                for (ResolveInfo resolveInfo : requireContext().getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)) {
                    requireContext().grantUriPermission(resolveInfo.activityInfo.packageName, zipURI, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                emailIntent.putExtra(Intent.EXTRA_STREAM, zipURI);
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Now showing chooser for sending debug logs to dev", emailIntent);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        findPreference("create_shortcut").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(requireContext(), LOG_TAG, preference.getKey() + " clicked");
                Intent i;
                LogFactory.writeMessage(requireContext(), LOG_TAG, "User wants to create a shortcut",
                        i = new Intent(requireContext(), ConfigureActivity.class).putExtra("creatingShortcut", true));
                startActivityForResult(i, REQUEST_CREATE_SHORTCUT);
                return true;
            }
        });
        findPreference("exclude_apps").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Set<String> apps = preferences.getStringSet("excluded_apps");
                startActivityForResult(new Intent(requireContext(), AppSelectionActivity.class).putExtra("apps", Collections.list(Collections.enumeration(apps))).
                        putExtra("infoTextWhitelist", getString(R.string.excluded_apps_info_text_whitelist)).putExtra("infoTextBlacklist", getString(R.string.excluded_apps_info_text_blacklist))
                        .putExtra("whitelist", preferences.getBoolean("excluded_whitelist", false)).putExtra("onlyInternet", true), REQUEST_EXCLUDE_APPS);
                return true;
            }
        });
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            ((PreferenceCategory) findPreference("general_category")).removePreference(findPreference("exclude_apps"));
        findPreference("reset").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                LogFactory.writeMessage(requireContext(), LOG_TAG, preference.getKey() + " clicked");
                new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext())).setTitle(R.string.warning).setMessage(R.string.reset_warning_text).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogFactory.writeMessage(requireContext(), LOG_TAG, "Resetting..");
                        Preferences.getDefaultPreferences(requireContext()).edit().clear().commit();
                        preferences.clearLocalStorage();
                        Util.deleteDatabase(requireContext());
                        LogFactory.writeMessage(requireContext(), LOG_TAG, "Reset finished.");
                        Util.getActivity(SettingsFragment.this).finish();
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
                String val = (String) newValue;
                int theme = Integer.parseInt(val);
                ThemeHandler.updateAppTheme(requireContext(), theme);
                ThemeHandler.updateDialogTheme(requireContext(), theme);
                IntentUtil.restartActivity(Util.getActivity(SettingsFragment.this));
                return true;
            }
        });
        findPreference("pin_app_shortcut").setOnPreferenceChangeListener(changeListener);
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N_MR1) {
            ((PreferenceCategory) findPreference("general_category")).removePreference(findPreference("setting_app_shortcuts_enabled"));
            ((PreferenceCategory) findPreference("pin_category")).removePreference(findPreference("pin_app_shortcut"));
        } else
            findPreference("setting_app_shortcuts_enabled").setOnPreferenceChangeListener(changeListener);
        findPreference("theme").setDefaultValue(0);
        LogFactory.writeMessage(requireContext(), LOG_TAG, "Done with onCreate");
        final CheckBoxPreference v4Enabled = (CheckBoxPreference) findPreference("setting_ipv4_enabled"),
                v6Enabled = (CheckBoxPreference) findPreference("setting_ipv6_enabled");
        v4Enabled.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, final Object newValue) {
                final boolean val = (boolean) newValue;
                if (!val){
                    new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext())).setNegativeButton(R.string.cancel, null).
                            setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    preferences.put("setting_ipv4_enabled", false);
                                    v6Enabled.setEnabled(false);
                                    if (Util.isServiceRunning(requireContext()))
                                        requireContext().startService(new Intent(requireContext(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                                    v4Enabled.setChecked(false);
                                }
                            }).setTitle(R.string.warning).setMessage(R.string.warning_disabling_v4).show();
                }else{
                    v6Enabled.setEnabled(true);
                    preferences.put("setting_ipv4_enabled", true);
                    if (Util.isServiceRunning(requireContext())) {
                        requireContext().startService(new Intent(requireContext(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                    }
                }
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
                boolean val = (boolean) newValue;
                v4Enabled.setEnabled(val);
               preferences.put(preference.getKey(), newValue);
                if (Util.isServiceRunning(requireContext()))
                    requireContext().startService(new Intent(requireContext(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true));
                return true;
            }
        });
        v4Enabled.setEnabled(v6Enabled.isChecked());
        v6Enabled.setEnabled(v4Enabled.isChecked());
        if (preferences.getBoolean("excluded_whitelist", false)) {
            findPreference("excluded_whitelist").setSummary(R.string.excluded_apps_info_text_whitelist);
        } else {
            findPreference("excluded_whitelist").setTitle(R.string.blacklist);
        }
        findPreference("excluded_whitelist").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean newValue = (boolean) o;
               preferences.put("app_whitelist_configured", true);
               preferences.put("excluded_whitelist", o);
                preference.setSummary(newValue ? R.string.excluded_apps_info_text_whitelist : R.string.excluded_apps_info_text_blacklist);
                preference.setTitle(newValue ? R.string.whitelist : R.string.blacklist);
                Set<String> selected = preferences.getStringSet("excluded_apps");
                Set<String> flipped = new HashSet<>();
                List<ApplicationInfo> packages = requireContext().getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
                for (ApplicationInfo packageInfo : packages) {
                    if (selected.contains(packageInfo.packageName)) continue;
                    flipped.add(packageInfo.packageName);
                }
               preferences.put("excluded_apps", flipped);
                return true;
            }
        });
        findPreference("setting_pin_enabled").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if ((boolean) newValue) {
                    if (preferences.getString("pin_value", "1234").equals("1234")) {
                        getPreferenceManager().showDialog(findPreference("pin_value"));
                        awaitingPinChange = true;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (awaitingPinChange && !DesignUtil.hasOpenDialogs(Util.getActivity(SettingsFragment.this))) {
                                    ((CheckBoxPreference) preference).setChecked(false);
                                    awaitingPinChange = false;
                                }
                                if (awaitingPinChange) handler.postDelayed(this, 250);
                            }
                        }, 250);
                    }
                    if (!((CheckBoxPreference) findPreference("pin_app")).isChecked())
                        ((CheckBoxPreference) findPreference("pin_app")).setChecked(true);
                }
                return true;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Preference pref = findPreference("setting_show_notification");
            ((CheckBoxPreference) pref).setChecked(true);
            pref.setSummary(pref.getSummary() + "\n" + getString(R.string.no_disable_android_o));
            pref.setEnabled(false);
            findPreference("show_used_dns").setDependency("");
            findPreference("hide_notification_icon").setDependency("");
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((MainActivity)requireContext(), new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_FINGERPRINT_PERMISSION);
            }else{
                FingerprintManager fingerprintManager = (FingerprintManager) requireContext().getSystemService(Context.FINGERPRINT_SERVICE);
                KeyguardManager keyguardManager = requireContext().getSystemService(KeyguardManager.class);
                if (fingerprintManager != null && !fingerprintManager.isHardwareDetected()) {
                    ((PreferenceCategory)findPreference("pin_category")).removePreference(findPreference("pin_fingerprint"));
                }else if(fingerprintManager == null || keyguardManager == null || !fingerprintManager.hasEnrolledFingerprints() || !keyguardManager.isKeyguardSecure()){
                    findPreference("pin_fingerprint").setDependency("");
                    findPreference("pin_fingerprint").setEnabled(false);
                }
            }
        }else{
            ((PreferenceCategory)findPreference("pin_category")).removePreference(findPreference("pin_fingerprint"));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            findPreference("jump_advanced_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(new Intent(requireContext(), AdvancedSettingsActivity.class), REQUEST_ADVANCED_SETTINGS);
                    return true;
                }
            });
        }else{
            getPreferenceScreen().removePreference(findPreference("category_advanced"));
        }
        findPreference("hide_notification_icon").setOnPreferenceChangeListener(changeListener);
        findPreference("pin_value").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(newValue.toString().equals("")){
                    ((CheckBoxPreference)findPreference("setting_pin_enabled")).setChecked(false);
                }else {
                    awaitingPinChange = false;
                    preferences.put("pin_value",String.valueOf(newValue));
                }
                return false;
            }
        });
        if(!PreferencesAccessor.isIPv6Enabled(requireContext()) && !preferences.getBoolean("ipv6_asked", false)){
            ipv6EnableQuestionSnackbar = Snackbar.make(((MainActivity)requireContext()).getDrawerLayout(), R.string.question_enable_ipv6, Snackbar.LENGTH_INDEFINITE);
            ipv6EnableQuestionSnackbar.setAction(R.string.yes, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    scrollToPreference("setting_ipv6_enabled"); //Scrolling to a lower preference because the wanted one would be at the bottom of the screen otherwise
                }
            });
            ipv6EnableQuestionSnackbar.show();
           preferences.put("ipv6_asked", true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_FINGERPRINT_PERMISSION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                findPreference("pin_fingerprint").setDependency("setting_pin_enabled");
                findPreference("pin_fingerprint").setEnabled(((CheckBoxPreference)findPreference("setting_pin_enabled")).isChecked());
            }
        }
    }

    private final Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            LogFactory.writeMessage(requireContext(), LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                    newValue + ", Type: " + Preferences.guessType(newValue));
            Preferences.getInstance(requireContext()).put(preference.getKey(), newValue, false);
            String key = preference.getKey();
            if((key.equalsIgnoreCase("setting_show_notification") || key.equalsIgnoreCase("show_used_dns") ||
                    key.equalsIgnoreCase("auto_pause") || key.equalsIgnoreCase("hide_notification_icon")) && Util.isServiceRunning(requireContext())){
                Intent i;
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Updating DNSVPNService, as a relevant setting " +
                        "(notification/autopause) changed", i = new Intent(requireContext(), DNSVpnService.class));
                requireContext().startService(i);
            }
            if(key.equals("pin_app_shortcut") || key.equals("setting_app_shortcuts_enabled")) Util.updateAppShortcuts(requireContext());
            return true;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        LogFactory.writeMessage(requireContext(), LOG_TAG, "Resuming Activity");
        if(devicePolicyManager.isAdminActive(deviceAdmin)){
            ((SwitchPreference)findPreference("device_admin")).setChecked(true);
        }
    }

    @Override
    public void onDestroy() {
        if(ipv6EnableQuestionSnackbar != null)ipv6EnableQuestionSnackbar.dismiss();
        automatingCategory = debugCategory = null;
        removeUsagePreference = sendDebugPreference = null;
        devicePolicyManager = null;
        deviceAdmin = null;
        ipv6EnableQuestionSnackbar = null;
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Preferences preferences = Preferences.getInstance(requireContext());
        LogFactory.writeMessage(requireContext(), LOG_TAG, "Received onActivityResult", data);
        if(requestCode == USAGE_STATS_REQUEST){
            LogFactory.writeMessage(requireContext(), LOG_TAG, "Got answer to the Usage Stats request");
            if(PermissionsUtil.hasUsageStatsPermission(requireContext())){
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Permission to usage stats was granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(true);
                preferences.put("auto_pause",true);
                if(usageRevokeHidden){
                    automatingCategory.addPreference(removeUsagePreference);
                    usageRevokeHidden = false;
                }
            }else{
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Permission to usage stats wasn't granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(false);
               preferences.put("auto_pause",false);
                if(!usageRevokeHidden){
                    LogFactory.writeMessage(requireContext(), LOG_TAG, "Access was previously granted, hiding 'Revoke access' preference");
                    automatingCategory.removePreference(removeUsagePreference);
                    usageRevokeHidden = true;
                }
            }
        }else if(requestCode == CHOOSE_AUTOPAUSEAPPS_REQUEST && resultCode == AppCompatActivity.RESULT_OK){
            LogFactory.writeMessage(requireContext(), LOG_TAG, "User returned from configuring autopause apps");
            ArrayList<String> apps = data.getStringArrayListExtra("apps");
            findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                    replace("[[count]]", ""+ apps.size()));
            if(apps.size() != getResources().getStringArray(R.array.default_blacklist).length)preferences.put("app_whitelist_configured", true);
           preferences.put("autopause_apps", new HashSet<>(apps));
           preferences.put("autopause_apps_count", apps.size());
            if(Util.isServiceRunning(requireContext())){
                Intent i;
                LogFactory.writeMessage(requireContext(), LOG_TAG, "Restarting DNSVPNService because the autopause apps changed",
                        i = new Intent(requireContext(), DNSVpnService.class));
                requireContext().startService(i);
            }
        }else if(requestCode == REQUEST_CODE_ENABLE_ADMIN && resultCode == AppCompatActivity.RESULT_OK && devicePolicyManager.isAdminActive(deviceAdmin)){
            LogFactory.writeMessage(requireContext(), LOG_TAG, "Deviceadmin was activated");
            ((SwitchPreference)findPreference("device_admin")).setChecked(true);
        }else if(requestCode == REQUEST_CREATE_SHORTCUT && resultCode == AppCompatActivity.RESULT_OK){
            final Snackbar snackbar = Snackbar.make(getListView(), R.string.shortcut_created, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    Utils.goToLauncher(requireContext());
                }
            });
            snackbar.show();
        }else if(requestCode == REQUEST_EXCLUDE_APPS && resultCode == AppCompatActivity.RESULT_OK){
            ArrayList<String> apps = data.getStringArrayListExtra("apps");
           preferences.put("excluded_apps", new HashSet<>(apps));
           preferences.put("excluded_whitelist", data.getBooleanExtra("whitelist",false));
            if(Util.isServiceRunning(requireContext())){
                requireContext().startService(new Intent(requireContext(), DNSVpnService.class).putExtra(VPNServiceArgument.COMMAND_START_VPN.getArgument(), true).
                        putExtra(VPNServiceArgument.FLAG_DONT_UPDATE_DNS.getArgument(),true));
            }
        }else if(requestCode == REQUEST_ADVANCED_SETTINGS && resultCode == AppCompatActivity.RESULT_FIRST_USER){
            IntentUtil.restartActivity(((MainActivity)requireContext()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_settings, menu);

        SearchManager searchManager = Utils.requireNonNull((SearchManager)requireContext().getSystemService(Context.SEARCH_SERVICE));
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(Util.getActivity(this).getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
    }

    private final Pattern emptySearchPattern = Pattern.compile("[\\s]*?");
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
