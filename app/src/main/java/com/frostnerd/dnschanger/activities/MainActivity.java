package com.frostnerd.dnschanger.activities;

import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.ThemeHandler;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.dnschanger.fragments.MainFragment;
import com.frostnerd.dnschanger.fragments.SettingsFragment;
import com.frostnerd.dnschanger.tasker.ConfigureActivity;
import com.frostnerd.utils.design.material.navigationdrawer.DrawerItem;
import com.frostnerd.utils.design.material.navigationdrawer.DrawerItemCreator;
import com.frostnerd.utils.design.material.navigationdrawer.NavigationDrawerActivity;
import com.frostnerd.utils.design.material.navigationdrawer.StyleOptions;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

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
    private SettingsFragment settingsFragment;
    private DrawerItem defaultDrawerItem, settingsDrawerItem;
    @ColorInt int backgroundColor;
    @ColorInt int textColor;
    private BroadcastReceiver shortcutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Snackbar snackbar = Snackbar.make(getContentFrame(), R.string.shortcut_created, Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(R.string.show, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                    Utils.goToLauncher(MainActivity.this);
                }
            });
            snackbar.show();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Receiver is not unregistered in onPause() because the app is in the background when a shortcut
        // is created
        registerReceiver(shortcutReceiver, new IntentFilter(API.BROADCAST_SHORTCUT_CREATED));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        backgroundColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.colorBackground);
        textColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.textColor);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        if(dialog1 != null && dialog1.isShowing())dialog1.cancel();
        if(defaultDnsDialog != null && defaultDnsDialog.isShowing())defaultDnsDialog.cancel();
        unregisterReceiver(shortcutReceiver);
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
        DrawerItemCreator itemCreator = new DrawerItemCreator(this);
        itemCreator.createItemAndContinue(R.string.nav_title_main);
        itemCreator.createItemAndContinue(R.string.nav_title_dns, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_home)), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment(@Nullable Bundle arguments) {
                return mainFragment == null ? mainFragment=new MainFragment() : mainFragment;
            }
        }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
            @Override
            public void access(DrawerItem item) {
                defaultDrawerItem = item;
            }
        });
        itemCreator.createItemAndContinue(R.string.settings, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_settings)), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment(@Nullable Bundle arguments) {
                if(settingsFragment == null)settingsFragment = new SettingsFragment();
                if(arguments != null)settingsFragment.setArguments(arguments);
                return settingsFragment;
            }
        }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
            @Override
            public void access(DrawerItem item) {
                settingsDrawerItem = item;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_learn);
        itemCreator.createItemAndContinue(R.string.nav_title_how_does_it_work, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_wrench)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                dialog1 = new AlertDialog.Builder(MainActivity.this, ThemeHandler.getDialogTheme(MainActivity.this)).setTitle(R.string.nav_title_how_does_it_work)
                        .setMessage(R.string.info_text_how_does_it_work).setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_what_is_dns, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_help)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                LogFactory.writeMessage(MainActivity.this, LOG_TAG, "Opening Dialog with info about DNS");
                dialog1 = new AlertDialog.Builder(MainActivity.this, ThemeHandler.getDialogTheme(MainActivity.this)).setTitle(R.string.info_dns_button).setMessage(R.string.info_text_dns).setCancelable(true).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
                LogFactory.writeMessage(MainActivity.this, LOG_TAG, "Dialog is now being shown");
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_features);
        itemCreator.createItemAndContinue(R.string.shortcuts, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_open_in_new)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                dialog1 = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.shortcuts).setNegativeButton(R.string.close, null)
                        .setPositiveButton("pos", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                openSettingsAndScrollToKey("shortcut_category");
                            }
                        })
                        .setNeutralButton(R.string.create_one, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int cnt) {
                                Intent i;
                                LogFactory.writeMessage(MainActivity.this, LOG_TAG, "User wants to create a shortcut",
                                        i = new Intent(MainActivity.this, ConfigureActivity.class).putExtra("creatingShortcut", true));
                                startActivityForResult(i,1);
                            }
                        }).setMessage(R.string.feature_shortcuts).create();
                dialog1.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        Button button = dialog1.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
                        button.setText("");
                        Drawable gear = setDrawableColor(getResources().getDrawable(R.drawable.ic_settings));
                        button.setCompoundDrawablesWithIntrinsicBounds(gear, null, null, null);
                    }
                });
                dialog1.show();
                return false;
            }
        });
        if(API.isTaskerInstalled(this)){
            itemCreator.createItemAndContinue(R.string.tasker_support, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_thumb_up)), new DrawerItem.ClickListener() {
                @Override
                public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                    return false;
                }
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            itemCreator.createItemAndContinue(R.string.nav_title_tiles, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_viewquilt)), new DrawerItem.ClickListener() {
                @Override
                public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                    dialog1 = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.nav_title_tiles).setMessage(R.string.feature_tiles)
                            .setNeutralButton(R.string.close, null).show();
                    return false;
                }
            });
        }
        itemCreator.createItemAndContinue(R.string.nav_title_pin_protection, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_action_key)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_more, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_ellipsis)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.app_name);
        itemCreator.createItemAndContinue(R.string.rate, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_star)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                rateApp();
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.title_share_app, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_share)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.app_share_text));
                LogFactory.writeMessage(MainActivity.this, LOG_TAG, "Showing chooser for share", sharingIntent);
                startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_using)));
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.contact_developer, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_person)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto","support@frostnerd.com", null));
                String body = "\n\n\n\n\n\n\nSystem:\nApp version: " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")\n"+
                        "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")";
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com");
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);
                LogFactory.writeMessage(MainActivity.this, LOG_TAG, "Now showing chooser for contacting dev", emailIntent);
                startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)));
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.title_about, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_info)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                String text = getString(R.string.about_text).replace("[[version]]", BuildConfig.VERSION_NAME).replace("[[build]]", BuildConfig.VERSION_CODE + "");
                new AlertDialog.Builder(MainActivity.this).setTitle(R.string.title_about).setMessage(text)
                        .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                return false;
            }
        });
        return itemCreator.getDrawerItems();
    }

    private void openSettingsAndScrollToKey(String key){
        Bundle arguments = new Bundle();
        arguments.putString(SettingsFragment.ARGUMENT_SCROLL_TO_SETTING, key);
        clickItem(settingsDrawerItem, arguments);
    }

    private Drawable setDrawableColor(Drawable drawable){
        drawable = drawable.mutate();
        drawable.setColorFilter(new LightingColorFilter(textColor, textColor));
        return drawable;
    }

    @Override
    public StyleOptions getStyleOptions() {
        return new StyleOptions(this).setListItemBackgroundColor(backgroundColor)
                .setSelectedListItemTextColor(textColor)
                .setSelectedListItemColor(ThemeHandler.getColor(this, R.attr.inputElementColor, -1))
                .setListItemTextColor(textColor)
                .setListViewBackgroundColor(backgroundColor);
    }

    public void rateApp() {
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
