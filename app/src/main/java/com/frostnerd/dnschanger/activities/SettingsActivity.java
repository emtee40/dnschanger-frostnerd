package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.design.FileChooserDialog;
import com.frostnerd.utils.preferences.AppCompatPreferenceActivity;
import com.frostnerd.utils.preferences.Preferences;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

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
    private PreferenceCategory automatingCategory;
    private Preference removeUsagePreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
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
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                String body = "\n\n\n\n\n\n\nSystem:\nApp version: " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")\n"+
                        "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")";
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return true;
            }
        });
        findPreference("auto_pause").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if(!((Boolean) newValue))return true;
                if(!API.hasUsageStatsPermission(SettingsActivity.this)){
                    new AlertDialog.Builder(SettingsActivity.this).setTitle(R.string.information).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_STATS_REQUEST);
                            dialog.cancel();
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setMessage(R.string.usage_stats_info_text).setCancelable(false).show();
                    return false;
                }else return true;
            }
        });
        findPreference("autopause_appselect").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.app_share_text));
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
                return true;
            }
        });
        findPreference("export_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                importSettings = false;
                exportSettings = false;
                if(checkWriteReadPermission())exportSettings();
                else exportSettings = true;
                return true;
            }
        });
        findPreference("import_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                importSettings = false;
                exportSettings = false;
                if(checkWriteReadPermission())importSettings();
                else exportSettings = true;
                return true;
            }
        });
    }

    private final int REQUEST_EXTERNAL_STORAGE = 815;
    private boolean exportSettings, importSettings;
    private boolean checkWriteReadPermission(){
        if(!canReadExternalStorage(this) || !canWriteExternalStorage(this)){
            new AlertDialog.Builder(this).setTitle(R.string.title_import_export).setMessage(R.string.explain_storage_permission).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    ActivityCompat.requestPermissions(SettingsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_EXTERNAL_STORAGE);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).show();
            return false;
        }
        return true;
    }

    private void importSettings(){
        new FileChooserDialog(SettingsActivity.this, false, FileChooserDialog.SelectionMode.FILE).setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                SettingsImportActivity.importFromFile(SettingsActivity.this, file);
                finish();
            }
        }).showDialog();
    }

    private void exportSettings(){
        FileChooserDialog dialog = new FileChooserDialog(SettingsActivity.this, true, FileChooserDialog.SelectionMode.DIR);
        dialog.setShowFiles(false);
        dialog.setShowDirs(true);
        dialog.setNavigateToLastPath(false);
        dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                Map<String,Object> all = Preferences.getAll(SettingsActivity.this);
                final File f = new File(file, "dnschanger.settings");
                if(f.exists())f.delete();
                FileWriter fw = null;
                BufferedWriter writer = null;
                try{
                    fw = new FileWriter(f);
                    writer = new BufferedWriter(fw);
                    writer.write("[DNSChanger Settings - " + BuildConfig.VERSION_NAME + "]\n");
                    writer.write("[Developer: Frostnerd.com]\n");
                    for(String s: all.keySet()) writer.write(s + "<->" + all.get(s) + "\n");
                    writer.flush();
                    new AlertDialog.Builder(SettingsActivity.this).setMessage(R.string.message_settings_exported).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setNeutralButton(R.string.open_share_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                            intentShareFile.setType("text/plain");
                            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ f.getPath()));
                            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings));
                            intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.settings));

                            startActivity(Intent.createChooser(intentShareFile, getString(R.string.open_share_file)));
                        }
                    }).setTitle(R.string.success).show();
                } catch (IOException e) {
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_EXTERNAL_STORAGE){
            if(importSettings && canReadExternalStorage(this)){
                importSettings();
            }else if(exportSettings && canReadExternalStorage(this) && canWriteExternalStorage(this)){
                exportSettings();
            }
            importSettings = false;
            exportSettings = false;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static boolean canWriteExternalStorage(Context context){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canReadExternalStorage(Context context){
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Preferences.put(SettingsActivity.this,preference.getKey(),newValue);
            String key = preference.getKey();
            if((key.equalsIgnoreCase("setting_show_notification") || key.equalsIgnoreCase("show_used_dns") ||
                    key.equalsIgnoreCase("auto_pause")) && API.checkVPNServiceRunning(SettingsActivity.this))startService(new Intent(SettingsActivity.this, DNSVpnService.class));
            return true;
        }
    };

    private final int USAGE_STATS_REQUEST = 13, CHOOSE_AUTOPAUSEAPPS_REQUEST = 14;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == USAGE_STATS_REQUEST){
            if(API.hasUsageStatsPermission(this)){
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(true);
                Preferences.put(this, "auto_pause",true);
                if(usageRevokeHidden){
                    automatingCategory.addPreference(removeUsagePreference);
                    usageRevokeHidden = false;
                }
            }else{
                ((CheckBoxPreference)findPreference("auto_pause")).setChecked(false);
                Preferences.put(this, "auto_pause",false);
                if(!usageRevokeHidden){
                    automatingCategory.removePreference(removeUsagePreference);
                    usageRevokeHidden = true;
                }
            }
        }else if(requestCode == CHOOSE_AUTOPAUSEAPPS_REQUEST && resultCode == RESULT_OK){
            findPreference("autopause_appselect").setTitle(getString(R.string.title_autopause_apps).
                    replace("[[count]]", ""+data.getIntExtra("count",0)));
            if(API.checkVPNServiceRunning(SettingsActivity.this))startService(new Intent(SettingsActivity.this, DNSVpnService.class));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
