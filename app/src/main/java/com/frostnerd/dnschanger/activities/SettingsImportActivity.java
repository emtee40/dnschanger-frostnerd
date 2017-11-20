package com.frostnerd.dnschanger.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.utils.preferences.Preferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class SettingsImportActivity extends Activity {
    private static final String LOG_TAG = "[SettingsImportActivity]";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHandler.getAppTheme(this));
        LogFactory.writeMessage(this, LOG_TAG, "Created activity", getIntent());
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_VIEW) && PermissionsUtil.canReadExternalStorage(this)) {
            Uri uri = intent.getData();
            LogFactory.writeMessage(this, LOG_TAG, "Importing from given URI: " + uri);
            try {
                importFromStream(this, this.getContentResolver().openInputStream(uri));
            } catch (FileNotFoundException e) {
                LogFactory.writeStackTrace(this, LogFactory.Tag.ERROR, e);
                e.printStackTrace();
            }
        }
        finish();
    }

    public static void importFromFile(Context c, File f) {
        LogFactory.writeMessage(c, LOG_TAG, "Importing from File: " + f);
        try {
            importFromStream(c, new FileInputStream(f));
        } catch (FileNotFoundException e) {
            LogFactory.writeStackTrace(c, LogFactory.Tag.ERROR, e);
            e.printStackTrace();
        }
    }

    public static void importFromStream(Context c, InputStream stream) {
        LogFactory.writeMessage(c, LOG_TAG, "Importing from Stream");
        InputStreamReader ir = null;
        BufferedReader reader = null;
        try {
            LogFactory.writeMessage(c, LOG_TAG, "Trying to create streams");
            ir = new InputStreamReader(stream);
            reader = new BufferedReader(ir);
            LogFactory.writeMessage(c, LOG_TAG, "Streams created.");
            String line;
            StringBuilder data = new StringBuilder();
            LogFactory.writeMessage(c, LOG_TAG, "Reading data");
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.startsWith("[")) continue;
                if(line.startsWith("'")){
                    //Util.createShortcut(c, Shortcut.fromString(line.split("'")[1]));
                }
                else data.append(line);
            }
            LogFactory.writeMessage(c, LOG_TAG, "Data read: " + data);
            Preferences.importFromStringAndPut(c, data.toString(), "<<>>");
            LogFactory.writeMessage(c, LOG_TAG, "Imported data and added to preferences.");
        } catch (Exception e) {
            LogFactory.writeStackTrace(c, LogFactory.Tag.ERROR, e);
            e.printStackTrace();
        } finally {
            try {
                if (ir != null) ir.close();
                if (reader != null) reader.close();
                if (stream != null) stream.close();
            } catch (Exception e) {

            }
            Intent i;
            LogFactory.writeMessage(c, LOG_TAG, "Restarting App",
                    i = new Intent(c, PinActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
            c.startActivity(i);
        }
    }
}
