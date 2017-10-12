package com.frostnerd.dnschanger.dialogs;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;

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
public class RuleImportDialog extends AlertDialog {
    private File file;

    public RuleImportDialog(@NonNull final Activity context) {
        super(context, ThemeHandler.getDialogTheme(context));
        setTitle(R.string.import_rules);
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_import_rules, null, false));
        content.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePermissionOrShowFileDialog(context);
            }
        });
    }

    private void handlePermissionOrShowFileDialog(Activity activity){
        if(ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            FileChooserDialog dialog = new FileChooserDialog(getContext(), false, FileChooserDialog.SelectionMode.FILE, ThemeHandler.getDialogTheme(getContext()));
            dialog.setFileListener(new FileChooserDialog.FileSelectedListener() {
                @Override
                public void fileSelected(File f, FileChooserDialog.SelectionMode selectionMode) {
                    file = f;
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
