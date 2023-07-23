package com.frostnerd.dnschanger.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.core.content.FileProvider;

import com.frostnerd.design.dialogs.FileChooserDialog;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.Shortcut;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Preferences;
import com.frostnerd.lifecycle.BaseDialog;

import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
public class ExportSettingsDialog extends BaseDialog {
    private static final String LOG_TAG = "[ExportSettingsDialog]";

    public ExportSettingsDialog(final Context context) {
        super(context, ThemeHandler.getDialogTheme(context));
        LogFactory.writeMessage(context, LOG_TAG, "Export directory dialog is now being shown");
        if(DatabaseHelper.getInstance(context).getCount(Shortcut.class) == 0){
            LogFactory.writeMessage(context, LOG_TAG, "User has no shortcuts. Not asking whether to export them.");
            dismiss();
            progressExport(context, false);
        }else{
            LogFactory.writeMessage(context, LOG_TAG, "Exporting settings. Asking in dialog whether shortcuts should be exported as well.");
            setTitle(R.string.shortcuts);
            setMessage(context.getString(R.string.dialog_question_export_shortcuts));
            setButton(BUTTON_POSITIVE, context.getString(R.string.yes), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancel();
                    progressExport(context, true);
                }
            });
            setButton(BUTTON_NEGATIVE, context.getString(R.string.no), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancel();
                    progressExport(context, false);
                }
            });
            setButton(BUTTON_NEUTRAL, context.getString(R.string.cancel), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancel();
                }
            });
            show();
            LogFactory.writeMessage(context, LOG_TAG, "Dialog is now being shown");
        }
    }

    @Override
    protected void destroy() {

    }

    private void progressExport(final Context context, final boolean exportShortcuts){
        FileChooserDialog dialog = new FileChooserDialog(context, true, FileChooserDialog.SelectionMode.DIR, ThemeHandler.getDialogTheme(context));
        dialog.setShowFiles(false);
        dialog.setShowDirs(true);
        dialog.setNavigateToLastPath(false);
        dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                LogFactory.writeMessage(context, LOG_TAG, "Dir selected: " + file);
                final File f = new File(file, "dnschanger.settings");
                LogFactory.writeMessage(context, LOG_TAG, "Writing to File " + f);
                if(f.exists())f.delete();
                FileWriter fw = null;
                BufferedWriter writer = null;
                try{
                    LogFactory.writeMessage(context, LOG_TAG, "Trying to open streams");
                    fw = new FileWriter(f);
                    writer = new BufferedWriter(fw);
                    LogFactory.writeMessage(context, LOG_TAG, "Stream opened. Starting to write");
                    writer.write("[DNSChanger Settings - " + BuildConfig.VERSION_NAME + "]\n");
                    writer.write("[Developer: Frostnerd.com]\n");
                    writer.write("[DO NOT TAMPER WITH THIS FILE]\n");
                    writer.write("[IT WAS AUTOGENERATED AND YOU MIGHT BREAK IT]\n");
                    LogFactory.writeMessage(context, LOG_TAG, "Flushing Headers");
                    writer.flush();
                    writer.write(Preferences.exportToJson(context));
                    LogFactory.writeMessage(context, LOG_TAG, "Flushing data");
                    writer.flush();
                    if(exportShortcuts){
                        LogFactory.writeMessage(context, LOG_TAG, "Exporting shortcuts aswell");
                        writer.write("\n");
                        List<Shortcut> shortcuts = DatabaseHelper.getInstance(context).getAll(Shortcut.class);
                        for(Shortcut shortcut: shortcuts){
                            writer.write("'" + shortcut.toString() + "'\n");
                        }
                        LogFactory.writeMessage(context, LOG_TAG, "Flushing shortcut data");
                        writer.flush();
                        LogFactory.writeMessage(context, LOG_TAG, "Exported " + shortcuts.size() + " Shortcuts");
                    }
                    LogFactory.writeMessage(context, LOG_TAG, "Finished writing");
                    new AlertDialog.Builder(context).setMessage(R.string.message_settings_exported).setCancelable(true).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(context, LOG_TAG, "User clicked cancel on Share/open of exported settings Dialog.");
                            dialog.cancel();
                        }
                    }).setNeutralButton(R.string.open_share_file, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogFactory.writeMessage(context, LOG_TAG, "User choose to share exported settings file");
                            Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                            intentShareFile.setType("application/pdf");
                            intentShareFile.putExtra(Intent.EXTRA_STREAM,  FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f));
                            intentShareFile.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings));
                            intentShareFile.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.settings));
                            LogFactory.writeMessage(context, LOG_TAG, "Opening share", intentShareFile);
                            context.startActivity(Intent.createChooser(intentShareFile, context.getString(R.string.open_share_file)));
                        }
                    }).setTitle(R.string.success).show();
                    LogFactory.writeMessage(context, LOG_TAG, "Showing Dialog offering the possibility to share exported settings file");
                } catch (IOException e) {
                    LogFactory.writeStackTrace(context, new String[]{LogFactory.Tag.ERROR.toString()}, e);
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if(writer != null)writer.close();
                        if(fw != null)fw.close();
                    }catch (IOException ignored){

                    }
                }

            }

            @Override
            public void multipleFilesSelected(File... files) {

            }
        });
        dialog.showDialog();
    }
}
