package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.frostnerd.database.orm.Entity;
import com.frostnerd.database.orm.parser.columns.Column;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.design.dialogs.FileChooserDialog;
import com.frostnerd.design.dialogs.LoadingDialog;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSQuery;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.database.entities.DNSRuleImport;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.general.permissions.PermissionsUtil;
import com.frostnerd.preferences.AppCompatPreferenceActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
                    permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                    ActivityCompat.requestPermissions(AdvancedSettingsActivity.this, permissions, REQUEST_READWRITE_PERMISSION);
                } else {
                    showExportQueriesDialog();
                }
                return true;
            }
        });
        findPreference("clear_queries").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showClearListDialog(R.string.title_clear_queries, DNSQuery.class);
                return true;
            }
        });
        findPreference("clear_local_rules").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showClearListDialog(R.string.title_clear_local_rules, DNSRuleImport.class, DNSRule.class);
                return true;
            }
        });
        findPreference("undo_rule_import").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showUndoRuleImportDialog();
                return true;
            }
        });
        findPreference("tcp_timeout").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    return Integer.parseInt(newValue.toString()) > 0;
                } catch (Exception ignored) {}
                return false;
            }
        });
        setUndoRuleImportStatus();
    }

    private void setUndoRuleImportStatus() {
        findPreference("undo_rule_import").setEnabled(DatabaseHelper.getInstance(this).getCount(DNSRuleImport.class) != 0);
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return Preferences.getInstance(this);
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
        new AlertDialog.Builder(this)
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

    @SafeVarargs
    private final void showClearListDialog(int title, final Class<? extends Entity>... entities){
        new AlertDialog.Builder(this).setTitle(title).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                for (Class<? extends Entity> entity : entities) {
                    DatabaseHelper.getInstance(AdvancedSettingsActivity.this).deleteAll(entity);
                }
                setUndoRuleImportStatus();
            }
        }).setNegativeButton(R.string.cancel, null).setMessage(R.string.dialog_are_you_sure).show();
    }

    private void showUndoRuleImportDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this));
        builder.setTitle(R.string.title_undo_rule_import);
        final HashMap<String, DNSRuleImport> imports = new HashMap<>();
        for (DNSRuleImport dnsRuleImport : DatabaseHelper.getInstance(this).getAll(DNSRuleImport.class)) {
            imports.put(dnsRuleImport.toString(), dnsRuleImport);
        }
        final String[] displayedTexts = imports.keySet().toArray(new String[0]);
        final Set<DNSRuleImport> selectedImports = new HashSet<>();

        builder.setNegativeButton(R.string.cancel, null);
        builder.setMultiChoiceItems(displayedTexts, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                String current = displayedTexts[which];
                DNSRuleImport dnsRuleImport = imports.get(current);
                if(selectedImports.contains(dnsRuleImport)) selectedImports.remove(dnsRuleImport);
                else selectedImports.add(dnsRuleImport);
            }
        });
        builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                undoImports(selectedImports);
            }
        });
        builder.show();
    }

    private void undoImports(final Collection<DNSRuleImport> imports){
        final LoadingDialog loadingDialog = new LoadingDialog(this, R.string.loading);
        loadingDialog.show();
        new Thread(){
            @Override
            public void run() {
                Column<DNSRuleImport> rowid = DatabaseHelper.getInstance(AdvancedSettingsActivity.this).getRowIDColumn(DNSRuleImport.class);
                for(DNSRuleImport dnsRuleImport: imports){
                    WhereCondition condition = WhereCondition.between(rowid,
                            String.valueOf(dnsRuleImport.getFirstInsert()),
                            String.valueOf(dnsRuleImport.getLastInsert()));

                    DatabaseHelper.getInstance(AdvancedSettingsActivity.this).delete(dnsRuleImport);
                    DatabaseHelper.getInstance(AdvancedSettingsActivity.this).delete(DNSRule.class, condition);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingDialog.dismiss();
                        loadingDialog.cancel();
                        setUndoRuleImportStatus();
                    }
                });
            }
        }.start();
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
