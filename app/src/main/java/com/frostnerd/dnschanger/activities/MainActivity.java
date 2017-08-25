package com.frostnerd.dnschanger.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.dnschanger.fragments.MainFragment;
import com.frostnerd.utils.design.material.navigationdrawer.DrawerItem;
import com.frostnerd.utils.design.material.navigationdrawer.NavigationDrawerActivity;
import com.frostnerd.utils.design.material.navigationdrawer.StyleOptions;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class MainActivity extends NavigationDrawerActivity {
    private static final String LOG_TAG = "[MainActivity]";
    private AlertDialog dialog1;
    private DefaultDNSDialog defaultDnsDialog;
    private MainFragment mainFragment;
    private List<DrawerItem> drawerItems = new ArrayList<>();
    private DrawerItem defaultDrawerItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        if(dialog1 != null && dialog1.isShowing())dialog1.cancel();
        if(defaultDnsDialog != null && defaultDnsDialog.isShowing())defaultDnsDialog.cancel();
        super.onDestroy();
    }

    @NonNull
    @Override
    public DrawerItem getDefaultItem() {
        return defaultDrawerItem;
    }

    @Override
    public DrawerItem onItemClicked(DrawerItem item) {
        return item;
    }

    @Override
    public List<DrawerItem> createDrawerItems() {
        drawerItems.add(new DrawerItem(this, R.string.nav_title_main));
        drawerItems.add(defaultDrawerItem = new DrawerItem(this, R.string.nav_title_dns, ThemeHandler.getDrawableFromTheme(this, R.attr.nav_icon_home), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment() {
                return mainFragment=new MainFragment();
            }
        }));
        return drawerItems;
    }

    @Override
    public StyleOptions getStyleOptions() {
        @ColorInt int backgroundColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.colorBackground);
        @ColorInt int textColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.textColor);
        return new StyleOptions(this).setListItemBackgroundColor(backgroundColor)
                .setSelectedListItemTextColor(textColor)
                .setSelectedListItemColor(ThemeHandler.getColor(this, R.attr.inputElementColor, -1))
                .setListItemTextColor(textColor)
                .setListViewBackgroundColor(backgroundColor);
    }

    public void openDNSInfoDialog(View v) {
        LogFactory.writeMessage(this, LOG_TAG, "Opening Dialog with info about DNS");
        dialog1 = new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setTitle(R.string.info_dns_button).setMessage(R.string.dns_info_text).setCancelable(true).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
        LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
    }

    public void rateApp(View v) {
        final String appPackageName = this.getPackageName();
        LogFactory.writeMessage(this, LOG_TAG, "Opening site to rate app");
        try {
            LogFactory.writeMessage(this, LOG_TAG, "Trying to open market");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            LogFactory.writeMessage(this, LOG_TAG, "Market was opened");
        } catch (android.content.ActivityNotFoundException e) {
            LogFactory.writeMessage(this, LOG_TAG, "Market not present. Opening with general ACTION_VIEW");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
        Preferences.put(this, "rated",true);
    }

    public void openDefaultDNSDialog(View v) {
        LogFactory.writeMessage(this, LOG_TAG, "Opening DefaultDNSDialog");
        defaultDnsDialog = new DefaultDNSDialog(this, ThemeHandler.getDialogTheme(this), new DefaultDNSDialog.OnProviderSelectedListener(){
            @Override
            public void onProviderSelected(String name, String dns1, String dns2, String dns1V6, String dns2V6) {
                if(mainFragment.settingV6){
                    if(!dns1V6.equals(""))mainFragment.dns1.setText(dns1V6);
                    mainFragment.dns2.setText(dns2V6);
                    if(!dns1.equals(""))Preferences.put(MainActivity.this, "dns1", dns1);
                    Preferences.put(MainActivity.this, "dns2", dns2);
                }else{
                    if(!dns1.equals(""))mainFragment.dns1.setText(dns1);
                    mainFragment.dns2.setText(dns2);
                    if(!dns1V6.equals(""))Preferences.put(MainActivity.this, "dns1-v6", dns1V6);
                    Preferences.put(MainActivity.this, "dns2-v6", dns2V6);
                }
            }
        });
        defaultDnsDialog.show();
        LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
    }
}
