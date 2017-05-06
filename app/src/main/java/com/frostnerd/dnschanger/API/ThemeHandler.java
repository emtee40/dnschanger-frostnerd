package com.frostnerd.dnschanger.API;

import android.content.Context;

import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Created by Daniel on 04.05.2017.
 */

public class ThemeHandler {

    public static void updateAppTheme(Context context, int theme){
        Preferences.put(context, "apptheme", theme);
    }

    public static void updateDialogTheme(Context context, int theme){
        Preferences.put(context, "dialogtheme", theme);
    }

    public static int getDialogTheme(Context context){
        return Preferences.getInteger(context, "dialogtheme", R.style.DialogTheme);
    }

    public static int getAppTheme(Context context){
        return Preferences.getInteger(context, "apptheme", R.style.AppTheme);
    }
}
