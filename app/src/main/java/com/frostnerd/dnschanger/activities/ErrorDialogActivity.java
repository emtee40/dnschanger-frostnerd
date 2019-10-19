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
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;

import java.io.File;

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
            AlertDialog alertDialog = new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setTitle(getString(R.string.error) + " - " + getString(R.string.app_name))
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
            }).setMessage(Html.fromHtml(getString(R.string.unfixable_error_explaination).replace("\n","\n<br>"))).show();
            TextView tv = alertDialog.findViewById(android.R.id.message);
            if(tv != null){
                Linkify.addLinks(tv, Linkify.ALL);
                tv.setLinksClickable(true);
                tv.setAutoLinkMask(Linkify.ALL);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
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
                    if(PreferencesAccessor.isDebugEnabled(ErrorDialogActivity.this)){
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
                i = new Intent(context, ErrorDialogActivity.class).putExtra("stacktrace",LogFactory.stacktraceToString(t)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        context.startActivity(i);
    }

    @Override
    public void finish() {
        super.finish();
        System.exit(1);
    }
}
