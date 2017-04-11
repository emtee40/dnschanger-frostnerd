package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.receivers.AdminReceiver;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.FileChooserDialog;
import com.frostnerd.utils.preferences.AppCompatPreferenceActivity;
import com.frostnerd.utils.preferences.Preferences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private boolean usageRevokeHidden = false;
    private PreferenceCategory automatingCategory, debugCategory;
    private Preference removeUsagePreference, sendDebugPreference;
    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;
    private final int REQUEST_EXTERNAL_STORAGE = 815,REQUEST_CODE_ENABLE_ADMIN = 1;
    private boolean exportSettings, importSettings;
    private final int USAGE_STATS_REQUEST = 13, CHOOSE_AUTOPAUSEAPPS_REQUEST = 14;
    private final static String LOG_TAG = "[SettingsActivity]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFactory.writeMessage(this, LOG_TAG, "Created Activity");
        addPreferencesFromResource(R.xml.preferences);
        LogFactory.writeMessage(this, LOG_TAG, "Added preferences from resources");
        devicePolicyManager = (DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(this, AdminReceiver.class);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findPreference("setting_start_boot").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_show_notification").setOnPreferenceChangeListener(changeListener);
        findPreference("show_used_dns").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_auto_mobile").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_pin_enabled").setOnPreferenceChangeListener(changeListener);
        findPreference("setting_disable_netchange").setOnPreferenceChangeListener(changeListener);
        findPreference("pin_value").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(getString(R.string.summary_pin_value).replace("[[x]]", ""+newValue));
                return true;
            }
        });
        findPreference("pin_value").setSummary(getString(R.string.summary_pin_value).replace("[[x]]", Preferences.getString(this, "pin_value", "1234")));
        if(API.isTaskerInstalled(this))findPreference("warn_automation_tasker").setSummary(R.string.summary_automation_warn);
        else ((PreferenceCategory)findPreference("automation")).removePreference(findPreference("warn_automation_tasker"));
        findPreference("setting_info").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.information).setMessage(R.string.settings_information_text).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
                return true;
            }
        });
        findPreference("contact_dev").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                String body = "\n\n\n\n\n\n\nSystem:\nApp version: " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")\n"+
                        "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")";
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Now showing chooser for contacting dev", emailIntent);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        findPreference("auto_pause").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                if(!((Boolean) newValue))return true;
                if(!API.hasUsageStatsPermission(SettingsActivity.this)){
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Access to usage stats is not yet granted. Showing dialog explaining why it's needed");
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.information).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i;
                            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User clicked OK in Usage stats access dialog, opening Usage Stats settings",
                                    i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            startActivityForResult(i, USAGE_STATS_REQUEST);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User cancelled the request for access to usage stats dialog");
                            dialog.cancel();
                        }
                    }).setMessage(R.string.usage_stats_info_text).setCancelable(false).show();
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Dialog is now being shown");
                    return false;
                }else return true;
            }
        });
        findPreference("autopause_appselect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                startActivityForResult(new Intent(SettingsActivity.this, AutoPauseAppSelectActivity.class),CHOOSE_AUTOPAUSEAPPS_REQUEST);
                return true;
            }
        });
        findPreference("placeholder_version").setSummary(getString(R.string.summary_version).replace("[[version]]", BuildConfig.VERSION_NAME).replace("[[code]]", BuildConfig.VERSION_CODE + ""));
        automatingCategory = (PreferenceCategory)getPreferenceScreen().findPreference("automation");
        removeUsagePreference = findPreference("remove_usage_data");
        removeUsagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_STATS_REQUEST);
                return true;
            }
        });
        boolean canAccessUsageStats = API.hasUsageStatsPermission(this);
        if(!canAccessUsageStats || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            usageRevokeHidden = true;
            automatingCategory.removePreference(removeUsagePreference);
            if(!canAccessUsageStats){
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(false);
                Preferences.put(this, "auto_pause",false);
            }
        }
        findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                replace("[[count]]", Preferences.getInteger(this, "autopause_apps_count",0) + ""));
        findPreference("share_app").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.app_share_text));
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Showing chooser for share", sharingIntent);
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
                return true;
            }
        });
        findPreference("export_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
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
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                importSettings = false;
                exportSettings = false;
                if(checkWriteReadPermission())importSettings();
                else exportSettings = true;
                return true;
            }
        });
        if(devicePolicyManager.isAdminActive(deviceAdmin)) ((SwitchPreference) findPreference("device_admin")).setChecked(true);
        else{
            ((SwitchPreference) findPreference("device_admin")).setChecked(false);
            Preferences.put(this,"device_admin", false);
        }
        findPreference("device_admin").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                boolean value = (Boolean)newValue;
                if(value && !devicePolicyManager.isAdminActive(deviceAdmin)){
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User wants app to function as DeviceAdmin but access isn't granted yet. Showing dialog explaining Device Admin");
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.information).setMessage(R.string.set_device_admin_info).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
                            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    getString(R.string.device_admin_description));
                            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User clicked OK in dialog explaining DeviceAdmin. Going to settings", intent);
                            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User chose to cancel the dialog explaining DeviceAdmin");
                            dialog.cancel();
                        }
                    }).show();
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Dialog is now being shown");
                    return false;
                }else if(!value){
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User disabled Admin access. Removing as Deviceadmin");
                    Preferences.put(SettingsActivity.this,"device_admin", false);
                    devicePolicyManager.removeActiveAdmin(deviceAdmin);
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "App was removed as DeviceAdmin");
                }else{
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User wants app to function as DeviceAdmin and Access was granted. Showing state as true.");
                    Preferences.put(SettingsActivity.this,"device_admin", true);
                }
                return true;
            }
        });
        sendDebugPreference = findPreference("send_debug");
        debugCategory = (PreferenceCategory)findPreference("debug_category");
        if(!Preferences.getBoolean(this, "debug",false))debugCategory.removePreference(sendDebugPreference);
        findPreference("debug").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preferences.put(SettingsActivity.this,preference.getKey(),newValue);
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                        newValue + ", Type: " + Preferences.getType(newValue));
                boolean val = (Boolean)newValue;
                if(!val){
                    debugCategory.removePreference(sendDebugPreference);
                    LogFactory.disable();
                    return true;
                }
                new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.warning).setMessage(R.string.debug_dialog_info_text).setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((SwitchPreference)findPreference("debug")).setChecked(true);
                                Preferences.put(SettingsActivity.this, "debug",true);
                                LogFactory.enable();
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
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, preference.getKey() + " clicked");
                File zip = LogFactory.zipLogFiles(SettingsActivity.this);
                if(zip == null)return true;
                Uri zipURI = FileProvider.getUriForFile(SettingsActivity.this,"com.frostnerd.dnschanger",zip);
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                for(ResolveInfo resolveInfo: getPackageManager().queryIntentActivities(emailIntent,PackageManager.MATCH_DEFAULT_ONLY)){
                    grantUriPermission(resolveInfo.activityInfo.packageName,zipURI, Intent.FLAG_GRANT_READ_URI_PERMISSION );
                }
                emailIntent.putExtra(Intent.EXTRA_STREAM, zipURI);
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Now showing chooser for sending debug logs to dev", emailIntent);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        LogFactory.writeMessage(this, LOG_TAG, "Done with onCreate");
    }

    private Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Preference " + preference.getKey() + " was changed to " +
                    newValue + ", Type: " + Preferences.getType(newValue));
            Preferences.put(SettingsActivity.this,preference.getKey(),newValue);
            String key = preference.getKey();
            if((key.equalsIgnoreCase("setting_show_notification") || key.equalsIgnoreCase("show_used_dns") ||
                    key.equalsIgnoreCase("auto_pause")) && API.checkVPNServiceRunning(SettingsActivity.this)){
                Intent i;
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Updating DNSVPNService, as a relevant setting " +
                        "(notification/autopause) changed", i = new Intent(SettingsActivity.this, DNSVpnService.class));
                startService(i);
            }
            return true;
        }
    };

    private boolean checkWriteReadPermission(){
        if(!API.canReadExternalStorage(this) || !API.canWriteExternalStorage(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Showing Dialog explaining why this app needs read/write access");
            new AlertDialog.Builder(this).setTitle(R.string.title_import_export).setMessage(R.string.explain_storage_permission).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User clicked OK in Read/Write dialog");
                    dialog.cancel();
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Requesting access to Read/Write Permission");
                    ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_EXTERNAL_STORAGE);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User choose to cancel Read/Write Request");
                    dialog.cancel();
                }
            }).show();
            return false;
        }
        return true;
    }

    private void importSettings(){
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Importing Setting. Showing chooser dialog.");
        new FileChooserDialog(SettingsActivity.this, false, FileChooserDialog.SelectionMode.FILE).setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG,"[IMPORTSETTINGS]"}, "User choose File " + file);
                LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Finishing Activity");
                finish();
                LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Starting import (Opening SettingsImportActivity");
                SettingsImportActivity.importFromFile(SettingsActivity.this, file);
            }
        }).showDialog();
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Dialog is now showing");
    }

    private void exportSettingsAskShortcuts(){
        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Exporting settings. Asking in dialog whether shortcuts should be exported aswell.");
        new AlertDialog.Builder(this).setTitle(R.string.shortcuts).setMessage(R.string.dialog_question_export_shortcuts).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                exportSettings(true);
            }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                exportSettings(false);
            }
        }).setCancelable(false).show();
        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Dialog is now being shown");
    }

    private void exportSettings(final boolean exportShortcuts){
        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Exporting settings. Showing chooser dialog.");
        FileChooserDialog dialog = new FileChooserDialog(SettingsActivity.this, true, FileChooserDialog.SelectionMode.DIR);
        dialog.setShowFiles(false);
        dialog.setShowDirs(true);
        dialog.setNavigateToLastPath(false);
        dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Dir selected: " + file);
                final File f = new File(file, "dnschanger.settings");
                LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Writing to File " + f);
                if(f.exists())f.delete();
                FileWriter fw = null;
                BufferedWriter writer = null;
                try{
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Trying to open streams");
                    fw = new FileWriter(f);
                    writer = new BufferedWriter(fw);
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Stream opened. Starting to write");
                    writer.write("[DNSChanger Settings - " + BuildConfig.VERSION_NAME + "]\n");
                    writer.write("[Developer: Frostnerd.com]\n");
                    writer.write("[DO NOT TAMPER WITH THIS FILE");
                    writer.write("[IT WAS AUTOGENERATED AND YOU MIGHT BREAK IT]");
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Flushing Headers");
                    writer.flush();
                    writer.write(Preferences.exportToString(SettingsActivity.this,false,"<<>>\n","first_run","device_admin"));
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Flushing data");
                    writer.flush();
                    if(exportShortcuts){
                        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Exporting shortcuts aswell");
                        API.Shortcut[] shortcuts = API.getShortcutsFromDatabase(SettingsActivity.this);
                        writer.write("\n");
                        for(API.Shortcut shortcut: shortcuts){
                            writer.write("'" + shortcut.toString() + "'\n");
                        }
                        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Flushing shortcut data");
                        writer.flush();
                        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Exported " + shortcuts.length + " Shortcuts");
                    }
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Finished writing");
                    new AlertDialog.Builder(SettingsActivity.this).setMessage(R.string.message_settings_exported).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "User clicked cancel on Share/open of exported settings Dialog.");
                            dialog.cancel();
                        }
                    }).setNeutralButton(R.string.open_share_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "User choose to share exported settings file");
                            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                            intentShareFile.setType("text/plain");
                            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ f.getPath()));
                            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings));
                            intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.settings));
                            LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Opening share", intentShareFile);
                            startActivity(Intent.createChooser(intentShareFile, getString(R.string.open_share_file)));
                        }
                    }).setTitle(R.string.success).show();
                    LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Showing Dialog offering the possibility to share exported settings file");
                } catch (IOException e) {
                    LogFactory.writeStackTrace(SettingsActivity.this, new String[]{LogFactory.Tag.ERROR.toString()}, e);
                    e.printStackTrace();
                }finally {
                    try {
                        if(writer != null)writer.close();
                        if(fw != null)fw.close();
                    }catch (IOException e){

                    }
                }
            }
        });
        dialog.showDialog();
        LogFactory.writeMessage(SettingsActivity.this, new String[]{LOG_TAG, "[EXPORTSETTINGS]"}, "Export directory dialog is now being shown");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Resuming Activity");
        if(devicePolicyManager.isAdminActive(deviceAdmin)){
            ((SwitchPreference)findPreference("device_admin")).setChecked(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Got Permission Request Result");
        if(requestCode == REQUEST_EXTERNAL_STORAGE){
            if(importSettings && API.canReadExternalStorage(this)){
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User wanted to import settings and granted the permissions");
                importSettings();
            }else if(exportSettings && API.canReadExternalStorage(this) && API.canWriteExternalStorage(this)){
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User wanted to export settings and granted the permissions");
                exportSettingsAskShortcuts();
            }else if(exportSettings || importSettings){
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User wants to import/export settings but hasn't granted the needed permissions.");
            }
            importSettings = false;
            exportSettings = false;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Received onActivityResult", data);
        if(requestCode == USAGE_STATS_REQUEST){
            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Got answer to the Usage Stats request");
            if(API.hasUsageStatsPermission(this)){
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Permission to usage stats was granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(true);
                Preferences.put(this, "auto_pause",true);
                if(usageRevokeHidden){
                    automatingCategory.addPreference(removeUsagePreference);
                    usageRevokeHidden = false;
                }
            }else{
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Permission to usage stats wasn't granted");
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(false);
                Preferences.put(this, "auto_pause",false);
                if(!usageRevokeHidden){
                    LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Access was previously granted, hiding 'Revoke access' preference");
                    automatingCategory.removePreference(removeUsagePreference);
                    usageRevokeHidden = true;
                }
            }
        }else if(requestCode == CHOOSE_AUTOPAUSEAPPS_REQUEST && resultCode == RESULT_OK){
            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "User returned from configuring autopause apps");
            findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                    replace("[[count]]", ""+data.getIntExtra("count",0)));
            if(API.checkVPNServiceRunning(SettingsActivity.this)){
                Intent i;
                LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Restarting DNSVPNService because the autopause apps changed",
                        i = new Intent(SettingsActivity.this, DNSVpnService.class));
                startService(i);
            }
        }else if(requestCode == REQUEST_CODE_ENABLE_ADMIN && resultCode == RESULT_OK && devicePolicyManager.isAdminActive(deviceAdmin)){
            LogFactory.writeMessage(SettingsActivity.this, LOG_TAG, "Deviceadmin was activated");
            ((SwitchPreference)findPreference("device_admin")).setChecked(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
