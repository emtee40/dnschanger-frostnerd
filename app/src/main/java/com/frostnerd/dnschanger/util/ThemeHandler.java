package com.frostnerd.dnschanger.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.util.TypedValue;

import com.frostnerd.dnschanger.R;
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

public class ThemeHandler {

    public static void updateAppTheme(Context context, int theme) {
        Preferences.getInstance(context).put( "apptheme", theme);
    }

    public static void updateDialogTheme(Context context, int theme) {
        Preferences.getInstance(context).put( "dialogtheme", theme);
    }

    public static int getDialogTheme(Context context) {
        return Preferences.getInstance(context).getInteger("dialogtheme", R.style.DialogTheme_Mono);
    }

    public static int getAppTheme(Context context) {
        return Preferences.getInstance(context).getInteger("apptheme", R.style.AppTheme_Mono);
    }

    @ColorInt
    public static int getColor(Context context, @AttrRes int attribute, @ColorInt int defaultValue) {
        TypedArray ta = context.obtainStyledAttributes(getAppTheme(context), new int[]{attribute});
        @ColorInt int color = ta.getColor(0, defaultValue);
        ta.recycle();
        return color;
    }

    public static int resolveThemeAttribute(Resources.Theme theme, @AttrRes int attribute){
        TypedValue value = new TypedValue();
        theme.resolveAttribute(attribute, value, true);
        return value.data;
    }

    public static Drawable getDrawableFromTheme(Context context, @AttrRes int attribute){
        TypedArray ta = context.obtainStyledAttributes(getAppTheme(context), new int[]{attribute});
        Drawable drawable = ta.getDrawable(0);
        ta.recycle();
        return drawable;
    }
}
