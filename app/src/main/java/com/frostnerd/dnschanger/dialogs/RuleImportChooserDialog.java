package com.frostnerd.dnschanger.dialogs;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.design.dialogs.FileChooserDialog;
import com.frostnerd.utils.design.dialogs.LoadingDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
class RuleImportChooserDialog extends AlertDialog {
    private final List<RuleImportProgressDialog.ImportableFile> files = new ArrayList<>();
    private final TextView fileLabel;
    private final TextView failFastInfo;
    private RuleImportProgressDialog.FileType type = RuleImportProgressDialog.FileType.DNSMASQ;
    private RuleImportProgressDialog importProgressDialog;
    private final CheckBox tryDetectType;
    private final CheckBox failFast;
    private final RadioButton dnsmasq;
    private final RadioButton hosts;
    private final RadioButton domains;
    private final RadioButton adblock;

    RuleImportChooserDialog(@NonNull final Activity context) {
        super(context, ThemeHandler.getDialogTheme(context));
        setTitle(R.string.import_rules);
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_import_rules, null, false));
        fileLabel = content.findViewById(R.id.text);
        tryDetectType = content.findViewById(R.id.detect_type);
        dnsmasq = content.findViewById(R.id.radio_dnsmasq);
        hosts = content.findViewById(R.id.radio_hosts);
        domains = content.findViewById(R.id.radio_justdomains);
        adblock = content.findViewById(R.id.radio_adblock);
        failFast = content.findViewById(R.id.fail_fast);
        failFastInfo = content.findViewById(R.id.fail_fast_info);
        content.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePermissionOrShowFileDialog(context);
            }
        });
        tryDetectType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                failFast.setEnabled(isChecked);
                failFastInfo.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        ((RadioGroup) content.findViewById(R.id.group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.radio_dnsmasq)
                    type = RuleImportProgressDialog.FileType.DNSMASQ;
                else if (checkedId == R.id.radio_hosts)
                    type = RuleImportProgressDialog.FileType.HOST;
                else if (checkedId == R.id.radio_justdomains)
                    type = RuleImportProgressDialog.FileType.DOMAIN_LIST;
                else if (checkedId == R.id.radio_adblock)
                    type = RuleImportProgressDialog.FileType.ADBLOCK_FILE;
            }
        });
        setButton(BUTTON_NEUTRAL, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        setButton(BUTTON_POSITIVE, getContext().getString(R.string.done), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                List<RuleImportProgressDialog.ImportableFile> importableFiles = new ArrayList<>();
                for (RuleImportProgressDialog.ImportableFile file : files) {
                    if (file.getFileType() != null) importableFiles.add(file);
                }
                dialog.dismiss();
                importProgressDialog = new RuleImportProgressDialog(context, importableFiles, SQLiteDatabase.CONFLICT_IGNORE);
                importProgressDialog.show();
            }
        });
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            }
        });
    }

    public void setActivityPaused(boolean paused){
        if(importProgressDialog != null)importProgressDialog.setHostingActivityPaused(paused);
    }

    private void handlePermissionOrShowFileDialog(Activity activity) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            FileChooserDialog dialog = new FileChooserDialog(getContext(), false, FileChooserDialog.SelectionMode.FILE, ThemeHandler.getDialogTheme(getContext()));
            dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
                @Override
                public void fileSelected(File f, FileChooserDialog.SelectionMode selectionMode) {
                    files.clear();
                    if (tryDetectType.isChecked()) detectFileTypes(f);
                }

                @Override
                public void multipleFilesSelected(File... selected) {
                    files.clear();
                    detectFileTypes(selected);
                }

                private void detectFileTypes(final File... selectedFiles) {
                    if (selectedFiles.length == 0) return;
                    if (selectedFiles.length == 1) {
                        detectSingleFileType(selectedFiles[0]);
                    } else {
                        getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                        final LoadingDialog dialog = new LoadingDialog(getContext(), ThemeHandler.getDialogTheme(getContext()), R.string.loading, R.string.wait_importing_rules);
                        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getContext().getString(R.string.cancel), new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        dialog.setOnCancelListener(new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                setLabelText();
                            }
                        });
                        dialog.show();
                        new Thread(){
                            @Override
                            public void run() {
                                int lines;
                                RuleImportProgressDialog.FileType type;
                                Handler handler = new Handler(Looper.getMainLooper());
                                for (final File f : selectedFiles) {
                                    if(!dialog.isShowing())break;
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.appendToMessage("\n\n" + f.getName());
                                        }
                                    });
                                    if ((lines = RuleImportProgressDialog.getFileLines(f)) == 0) continue;
                                    type = RuleImportProgressDialog.tryFindFileType(f, failFast.isChecked());
                                    files.add(new RuleImportProgressDialog.ImportableFile(f, type, lines));
                                }
                                dialog.cancel();
                            }
                        }.start();
                    }
                }

                private void detectSingleFileType(final File file){
                    final int lines;
                    if ((lines = RuleImportProgressDialog.getFileLines(file)) == 0) {
                        getButton(BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                        return;
                    }
                    getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                    final LoadingDialog dialog = new LoadingDialog(getContext(), ThemeHandler.getDialogTheme(getContext()),
                            R.string.loading, R.string.wait_importing_rules);
                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getContext().getString(R.string.cancel), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialog.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (type != null) switch (type) {
                                case DNSMASQ:
                                    dnsmasq.setChecked(true);
                                    break;
                                case HOST:
                                    hosts.setChecked(true);
                                    break;
                                case DOMAIN_LIST:
                                    domains.setChecked(true);
                                    break;
                                case ADBLOCK_FILE:
                                    adblock.setChecked(true);
                                    break;
                            }
                            files.add(new RuleImportProgressDialog.ImportableFile(file, type, lines));
                            setLabelText();
                        }
                    });
                    dialog.show();
                    new Thread(){
                        @Override
                        public void run() {
                            type = RuleImportProgressDialog.tryFindFileType(file, failFast.isChecked());
                            dialog.cancel();
                        }
                    }.start();
                }

                private void setLabelText() {
                    StringBuilder builder = new StringBuilder();
                    for (RuleImportProgressDialog.ImportableFile importableFile : files) {
                        builder.append(importableFile.getFile().getName()).append(" [").
                                append(importableFile.getFileType() == null ? getContext().getString(R.string.rule_unknown_ignoring) : importableFile.getFileType()).
                                append(", ").append(getContext().getString(R.string.x_lines).replace("[x]", "" + importableFile.getLines())).
                                append("]").append("\n");
                    }
                    fileLabel.setText(builder.toString());
                    if (files.size() == 0) getButton(BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                    else getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                }
            });
            dialog.setCanSelectMultiple(true);
            dialog.setNavigateToLastPath(true);
            dialog.showDialog();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 999);
            }
        }
    }
}
