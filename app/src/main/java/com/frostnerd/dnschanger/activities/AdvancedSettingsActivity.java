package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;

import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.design.dialogs.FileChooserDialog;
import com.frostnerd.utils.design.dialogs.LoadingDialog;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.utils.preferences.AppCompatPreferenceActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class AdvancedSettingsActivity extends AppCompatPreferenceActivity {
    private boolean dialogShown = false;
    private static final int REQUEST_READWRITE_PERMISSION = 919;
    private static final String LOG_TAG = "[AdvancedSettingsActivity]";
    private Thread exportQueriesThread;
    private LoadingDialog exportLoadingDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getPreferenceTheme(this));
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        findPreference("advanced_settings").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("advanced_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final SwitchPreference pref = (SwitchPreference)preference;
                if(pref.isChecked()){
                    pref.setChecked(false);
                    showWarrantyDialog();
                }
                return true;
            }
        });
        findPreference("custom_port").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("rules_activated").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("query_logging").setOnPreferenceChangeListener(preferenceChangeListener);
        findPreference("export_queries").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (!PermissionsUtil.canWriteExternalStorage(AdvancedSettingsActivity.this) ||
                        !PermissionsUtil.canReadExternalStorage(AdvancedSettingsActivity.this)) {
                    String[] permissions;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                    } else {
                        permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    }
                    ActivityCompat.requestPermissions(AdvancedSettingsActivity.this, permissions, REQUEST_READWRITE_PERMISSION);
                } else {
                    showExportQueriesDialog();
                }
                return true;
            }
        });
        final CheckBoxPreference tls = (CheckBoxPreference) findPreference("dns_over_tls"),
                tcp = (CheckBoxPreference) findPreference("dns_over_tcp");
        tls.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean val = (boolean)newValue;
                tcp.setEnabled(!val);
                return preferenceChangeListener.onPreferenceChange(preference, newValue);
            }
        });
        tcp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean val = (boolean)newValue;
                tls.setEnabled(!val);
                return preferenceChangeListener.onPreferenceChange(preference, newValue);
            }
        });

        if(tls.isChecked() && tls.isEnabled())tcp.setEnabled(false);
        else if(tcp.isChecked() && tcp.isEnabled())tls.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(exportQueriesThread != null){
            exportQueriesThread.interrupt();
            exportQueriesThread = null;
        }
        if(exportLoadingDialog != null){
            exportLoadingDialog.dismiss();
            exportLoadingDialog = null;
        }
    }

    private final Preference.OnPreferenceChangeListener preferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            setResult(RESULT_FIRST_USER);
            if (preference.getKey().equals("advanced_settings")) {
                if(!dialogShown && ((Boolean)o)) {
                    showWarrantyDialog();
                    return false;
                }
            }
            Preferences.getInstance(AdvancedSettingsActivity.this).put(preference.getKey(), o, false);
            return true;
        }
    };

    private void showWarrantyDialog(){
        dialogShown = true;
        ((SwitchPreference)findPreference("advanced_settings")).setChecked(false);
        new AlertDialog.Builder(AdvancedSettingsActivity.this).setTitle(R.string.warning).setMessage(R.string.information_advanced_settings_warranty).setCancelable(true).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ((SwitchPreference)findPreference("advanced_settings")).setChecked(true);
                        setResult(RESULT_FIRST_USER);
                        dialogShown = false;
                        dialog.dismiss();
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogShown = false;
            }
        }).show();
    }

    private void showExportQueriesDialog(){
        FileChooserDialog dialog = new FileChooserDialog(this, true,
                FileChooserDialog.SelectionMode.DIR, ThemeHandler.getDialogTheme(this));
        dialog.setShowFiles(false);
        dialog.setShowDirs(true);
        dialog.setNavigateToLastPath(false);
        dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Dir selected: " + file);
                final File f = new File(file, "dnschanger_queries_export.txt");
                LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Writing to File " + f);
                if(f.exists())f.delete();
                exportLoadingDialog = new LoadingDialog(AdvancedSettingsActivity.this, R.string.loading, R.string.loading_exporting_queries);
                exportLoadingDialog.setClosesWithLifecycle(false);
                exportQueriesThread = new Thread(){
                    @Override
                    public void run() {
                        FileWriter fw = null;
                        BufferedWriter writer = null;
                        int lines = 0;
                        try{
                            LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Trying to open streams");
                            fw = new FileWriter(f);
                            writer = new BufferedWriter(fw);
                            LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Stream opened. Starting to write");
                            SimpleDateFormat format = new SimpleDateFormat("[dd-MM-yyyy HH:mm]");
                            writer.write("# Format: [dd-MM-yyyy HH:mm] [epoch time] [AAAA/A]: host\n");
                            StringBuilder line = new StringBuilder();
                            for(DNSQuery query: DatabaseHelper.getInstance(AdvancedSettingsActivity.this).getAll(DNSQuery.class)){
                                if(isInterrupted())break;
                                line.setLength(0);
                                line.append(format.format(query.getTime()));
                                line.append(" [").append(System.currentTimeMillis()).append("]");
                                line.append(" [").append(query.isIpv6() ? "AAAA" : "A").append("]");
                                line.append(": ").append(query.getHost());
                                writer.write(line.toString());
                                writer.write("\n");
                                lines++;
                                if(lines % 500 == 0){
                                    LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Flushing data");
                                    writer.flush();
                                }
                            }
                            LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Flushing data");
                            writer.flush();
                            LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Finished writing");
                        } catch (IOException e) {
                            LogFactory.writeStackTrace(AdvancedSettingsActivity.this, new String[]{LogFactory.Tag.ERROR.toString()}, e);
                            e.printStackTrace();
                        } finally {
                            try {
                                if(writer != null)writer.close();
                                if(fw != null)fw.close();
                            }catch (IOException ignored){

                            }
                            final int finalLines = lines;
                            exportQueriesThread = null;
                            if(!isInterrupted()) runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    exportLoadingDialog.dismiss();
                                    showQueryExportSuccessDialog(f, finalLines);
                                    exportLoadingDialog = null;
                                }
                            });
                            else exportLoadingDialog = null;
                        }
                    }
                };
                exportQueriesThread.start();
                exportLoadingDialog.show();
            }

            @Override
            public void multipleFilesSelected(File... files) {

            }
        });
        dialog.showDialog();
    }

    private void showQueryExportSuccessDialog(final File f, int queries) {
        new android.app.AlertDialog.Builder(this)
                .setMessage(getString(R.string.exported_dns_queries).replace("[x]", String.valueOf(queries)))
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "User clicked cancel on Share/open of exported queries Dialog.");
                        dialog.cancel();
                    }
                }).setNeutralButton(R.string.open_share_file, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "User choose to share exported queries file");
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.setType("application/pdf");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(AdvancedSettingsActivity.this, BuildConfig.APPLICATION_ID + ".provider", f));
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.nav_title_dns_query));
                intentShareFile.putExtra(Intent.EXTRA_TEXT, getString(R.string.nav_title_dns_query));
                LogFactory.writeMessage(AdvancedSettingsActivity.this, LOG_TAG, "Opening share", intentShareFile);
                startActivity(Intent.createChooser(intentShareFile, getString(R.string.open_share_file)));
            }
        }).setTitle(R.string.success).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READWRITE_PERMISSION) {
            if (PermissionsUtil.canWriteExternalStorage(AdvancedSettingsActivity.this) &&
                    PermissionsUtil.canReadExternalStorage(AdvancedSettingsActivity.this)) {
                showExportQueriesDialog();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
