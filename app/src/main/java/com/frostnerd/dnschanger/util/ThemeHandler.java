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
        switch(Preferences.getInstance(context).getInteger("dialogtheme", 2)) {
            case 1: return R.style.DialogTheme;
            case 2: return R.style.DialogTheme_Mono;
            case 3: return R.style.DialogTheme_Dark;
            case 4: return R.style.DialogTheme_True_Black;
            case R.style.AppTheme: Preferences.getInstance(context).put("dialogtheme", 1); return R.style.DialogTheme;
            case R.style.AppTheme_Mono: Preferences.getInstance(context).put("dialogtheme", 2); return R.style.DialogTheme_Mono;
            case R.style.AppTheme_Dark: Preferences.getInstance(context).put("dialogtheme", 3); return R.style.DialogTheme_Dark;
        }
        return Preferences.getInstance(context).getInteger("dialogtheme", R.style.DialogTheme_Mono);
    }

    public static int getAppTheme(Context context) {
        switch(Preferences.getInstance(context).getInteger("apptheme", 2)) {
            case 1: return R.style.AppTheme;
            case 2: return R.style.AppTheme_Mono;
            case 3: return R.style.AppTheme_Dark;
            case 4: return R.style.AppTheme_True_Black;
            case R.style.AppTheme: Preferences.getInstance(context).put("apptheme", 1); return R.style.AppTheme;
            case R.style.AppTheme_Mono: Preferences.getInstance(context).put("apptheme", 2); return R.style.AppTheme_Mono;
            case R.style.AppTheme_Dark: Preferences.getInstance(context).put("apptheme", 3); return R.style.AppTheme_Dark;
        }
        return Preferences.getInstance(context).getInteger("apptheme", R.style.AppTheme_Mono);
    }

    public static int getPreferenceTheme(Context context){
        switch(getAppTheme(context)){
            case R.style.AppTheme_Mono: return R.style.PreferenceTheme_Mono;
            case R.style.AppTheme_Dark: return R.style.PreferenceTheme_Dark;
            case R.style.AppTheme_True_Black: return R.style.PreferenceTheme_True_Black;
            default: return R.style.PreferenceTheme;
        }
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

    @ColorInt
    public static int getSelectedItemColor(Context context){
        return getColor(context, R.attr.inputElementColor, -1);
    }

    public static Drawable getDrawableFromTheme(Context context, @AttrRes int attribute){
        TypedArray ta = context.obtainStyledAttributes(getAppTheme(context), new int[]{attribute});
        Drawable drawable = ta.getDrawable(0);
        ta.recycle();
        return drawable;
    }
}
