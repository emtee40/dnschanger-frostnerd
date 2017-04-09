package com.frostnerd.dnschanger.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_VIEW) && SettingsActivity.canReadExternalStorage(this)) {
            Uri uri = intent.getData();
            try {
                importFromStream(this, this.getContentResolver().openInputStream(uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        finish();
    }

    public static void importFromFile(Context c, File f) {
        try {
            importFromStream(c, new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void importFromStream(Context c, InputStream stream) {
        InputStreamReader ir = null;
        BufferedReader reader = null;
        try {
            ir = new InputStreamReader(stream);
            reader = new BufferedReader(ir);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("") || line.startsWith("[") || !line.contains("<->")) continue;
                Preferences.put(c, line.split("<->")[0], line.split("<->")[1]);
            }
        } catch (Exception e) {

        } finally {
            try {
                if (ir != null) ir.close();
                if (reader != null) reader.close();
                if (stream != null) stream.close();
            } catch (Exception e) {

            }
            c.startActivity(new Intent(c, PinActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }
}
