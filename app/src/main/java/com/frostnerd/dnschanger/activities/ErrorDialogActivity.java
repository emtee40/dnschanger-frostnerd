package com.frostnerd.dnschanger.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;

import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.preferences.Preferences;

import java.io.File;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class ErrorDialogActivity extends Activity {
    private static final String LOG_TAG = "[ErrorDialogActivity]";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHandler.getDialogTheme(this));
        LogFactory.writeMessage(this, LOG_TAG,"Created Activity", getIntent());
        final String crashReport = getIntent() != null ? getIntent().getStringExtra("stacktrace") : "";
        LogFactory.writeMessage(this, LOG_TAG,"Creating Dialog displaying the user that an error occurred");
        if(crashReport.contains("Cannot create interface")){ //Kind of dirty, but this error is unfixable
            new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setTitle(getString(R.string.error) + " - " + getString(R.string.app_name)).setMessage(R.string.unfixable_error_explaination)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            finish();
                        }
                    }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            }).show();
        }else{
            new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setTitle(getString(R.string.error) + " - " + getString(R.string.app_name)).setMessage(R.string.vpn_error_explain)
                    .setCancelable(false).setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LogFactory.writeMessage(ErrorDialogActivity.this, LOG_TAG,"User choose to cancel action");
                    dialog.cancel();
                    finish();
                }
            }).setNeutralButton(R.string.send_crash_report, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto","support@frostnerd.com", null));
                    String body = "\n\n\n\n\n\n\nSystem:\nApp version: " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")\n"+
                            "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")\n\n\nStacktrace:\n" + crashReport;
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - crash");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                    LogFactory.writeMessage(ErrorDialogActivity.this, LOG_TAG,"User choose to send Email to dev", emailIntent);
                    if(Preferences.getBoolean(ErrorDialogActivity.this, "debug",false)){
                        File zip = LogFactory.zipLogFiles(ErrorDialogActivity.this);
                        if(zip != null){
                            Uri zipURI = FileProvider.getUriForFile(ErrorDialogActivity.this,"com.frostnerd.dnschanger",zip);
                            for(ResolveInfo resolveInfo: getPackageManager().queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)){
                                grantUriPermission(resolveInfo.activityInfo.packageName,zipURI, Intent.FLAG_GRANT_READ_URI_PERMISSION );
                            }
                            emailIntent.putExtra(Intent.EXTRA_STREAM, zipURI);
                            emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                    finish();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            }).show();
        }
        LogFactory.writeMessage(this, LOG_TAG,"Showing Dialog");
    }

    public static void show(Context context, Throwable t){
        Intent i;
        LogFactory.writeMessage(context, new String[]{LOG_TAG, LogFactory.STATIC_TAG} , "Showing Stacktrace for " + t.getMessage(),
                i = new Intent(context, ErrorDialogActivity.class).putExtra("stacktrace",LogFactory.stacktraceToString(t)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
        context.startActivity(i);
    }
}
