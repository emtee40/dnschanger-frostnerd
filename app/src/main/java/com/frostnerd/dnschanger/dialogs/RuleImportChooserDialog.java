package com.frostnerd.dnschanger.dialogs;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.design.dialogs.FileChooserDialog;

import java.io.File;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RuleImportChooserDialog extends AlertDialog {
    private File file;
    private TextView fileLabel;
    private RuleImportProgressDialog.FileType type = RuleImportProgressDialog.FileType.DNSMASQ;
    private CheckBox tryDetectType;
    private RadioButton dnsmasq,hosts, domains;

    public RuleImportChooserDialog(@NonNull final Activity context) {
        super(context, ThemeHandler.getDialogTheme(context));
        setTitle(R.string.import_rules);
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_import_rules, null, false));
        fileLabel = content.findViewById(R.id.text);
        tryDetectType = content.findViewById(R.id.detect_type);
        dnsmasq = content.findViewById(R.id.radio_dnsmasq);
        hosts = content.findViewById(R.id.radio_hosts);
        domains = content.findViewById(R.id.radio_justdomains);
        content.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePermissionOrShowFileDialog(context);
            }
        });
        ((RadioGroup)content.findViewById(R.id.group)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if(checkedId == R.id.radio_dnsmasq)type = RuleImportProgressDialog.FileType.DNSMASQ;
                else if(checkedId == R.id.radio_hosts)type = RuleImportProgressDialog.FileType.HOST;
                else if(checkedId == R.id.radio_justdomains)type = RuleImportProgressDialog.FileType.DOMAIN_LIST;
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
                dialog.dismiss();
                new RuleImportProgressDialog(context, file, type).show();
            }
        });
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            }
        });
    }

    private void handlePermissionOrShowFileDialog(Activity activity){
        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            FileChooserDialog dialog = new FileChooserDialog(getContext(), false, FileChooserDialog.SelectionMode.FILE, ThemeHandler.getDialogTheme(getContext()));
            dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
                @Override
                public void fileSelected(File f, FileChooserDialog.SelectionMode selectionMode) {
                    if(RuleImportProgressDialog.getFileLines(f) == 0){
                        getButton(BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
                        fileLabel.setText("");
                    }else{
                        file = f;
                        fileLabel.setText(f.getName());
                        getButton(BUTTON_POSITIVE).setVisibility(View.VISIBLE);
                        if(tryDetectType.isChecked()){
                            RuleImportProgressDialog.FileType fileType = RuleImportProgressDialog.tryFindFileType(f);
                            if(fileType != null){
                                type = fileType;
                                switch (type){
                                    case DNSMASQ: dnsmasq.setChecked(true);break;
                                    case HOST: hosts.setChecked(true);break;
                                    case DOMAIN_LIST: domains.setChecked(true);
                                }
                            }
                        }
                    }
                }
            });
            dialog.setNavigateToLastPath(true);
            dialog.showDialog();
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 999);
            }
        }
    }
}
