package com.frostnerd.dnschanger.API;

import android.content.Context;

import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.preferences.Preferences;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
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
