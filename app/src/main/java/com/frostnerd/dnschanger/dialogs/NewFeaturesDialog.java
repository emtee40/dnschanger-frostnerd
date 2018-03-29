package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class NewFeaturesDialog extends AlertDialog {

    public NewFeaturesDialog(@NonNull Context context) {
        super(context, ThemeHandler.getDialogTheme(context));
        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setButton(BUTTON_POSITIVE, context.getString(R.string.close), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        setTitle(R.string.new_features);
        View content;
        setView(content = getLayoutInflater().inflate(R.layout.dialog_new_features, null, false));
        ((TextView)content.findViewById(R.id.text)).setText(BuildConfig.VERSION_NAME);
        ListView list = content.findViewById(R.id.list);
        String[] arr = context.getResources().getStringArray(getStringArrayID(context));
        for(int i = 0; i < arr.length; i++){
            arr[i] = "- " + arr[i];
        }
        list.setAdapter(new ArrayAdapter<>(context, R.layout.item_new_feature, arr));
        Preferences.getInstance(context).put( "features_" + BuildConfig.VERSION_NAME.replace(".", "_"),true);
    }

    private static int getStringArrayID(Context context){
        return context.getResources().getIdentifier("features_" + BuildConfig.VERSION_NAME.replace(".", "_"),
                "array", context.getPackageName());
    }

    public static boolean shouldShowDialog(Context context) {
        return getStringArrayID(context) != 0 && !Preferences.getInstance(context).getBoolean("features_" + BuildConfig.VERSION_NAME.replace(".", "_"), false);
    }
}
