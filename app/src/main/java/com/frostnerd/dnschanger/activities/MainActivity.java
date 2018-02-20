package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.dialogs.ExportSettingsDialog;
import com.frostnerd.dnschanger.fragments.CurrentNetworksFragment;
import com.frostnerd.dnschanger.fragments.QueryLogFragment;
import com.frostnerd.dnschanger.fragments.RulesFragment;
import com.frostnerd.dnschanger.services.RuleImportService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.RuleImport;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.BuildConfig;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.dialogs.DefaultDNSDialog;
import com.frostnerd.dnschanger.fragments.DnsQueryFragment;
import com.frostnerd.dnschanger.fragments.MainFragment;
import com.frostnerd.dnschanger.fragments.SettingsFragment;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.tasker.ConfigureActivity;
import com.frostnerd.utils.design.dialogs.FileChooserDialog;
import com.frostnerd.utils.design.dialogs.LoadingDialog;
import com.frostnerd.utils.design.material.navigationdrawer.DrawerItem;
import com.frostnerd.utils.design.material.navigationdrawer.DrawerItemCreator;
import com.frostnerd.utils.design.material.navigationdrawer.NavigationDrawerActivity;
import com.frostnerd.utils.design.material.navigationdrawer.StyleOptions;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.permissions.PermissionsUtil;
import com.frostnerd.utils.preferences.Preferences;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class MainActivity extends NavigationDrawerActivity implements RuleImport.ImportStartedListener {
    private static final String LOG_TAG = "[MainActivity]";
    private static final int REQUEST_PERMISSION_IMPORT_SETTINGS = 131, REQUEST_PERMISSION_EXPORT_SETTINGS = 130;
    private AlertDialog dialog1;
    private DefaultDNSDialog defaultDnsDialog;
    private MainFragment mainFragment;
    private SettingsFragment settingsFragment;
    private DrawerItem defaultDrawerItem, settingsDrawerItem;
    @ColorInt private int backgroundColor;
    @ColorInt private int textColor, navDrawableColor;
    private boolean startedActivity = false;
    private final BroadcastReceiver shortcutReceiver = new BroadcastReceiver() {
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
    public static MainActivity currentContext;
    private BroadcastReceiver importFinishedReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        startedActivity = false;
        // Receiver is not unregistered in onPause() because the app is in the background when a shortcut
        // is created
        registerReceiver(shortcutReceiver, new IntentFilter(Util.BROADCAST_SHORTCUT_CREATED));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHandler.getAppTheme(this));
        backgroundColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.colorBackground);
        textColor = ThemeHandler.resolveThemeAttribute(getTheme(), android.R.attr.textColor);
        navDrawableColor = ThemeHandler.resolveThemeAttribute(getTheme(), R.attr.navDrawableColor);
        super.onCreate(savedInstanceState);
        currentContext = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Util.getDBHelper(MainActivity.this);
            }
        }).start();
        Util.updateAppShortcuts(this);
        Util.runBackgroundConnectivityCheck(this);
        Preferences.put(this, "first_run", false);
        if(Preferences.getBoolean(this, "first_run", true)) Preferences.put(this, "excluded_apps", new ArraySet<>(Arrays.asList(getResources().getStringArray(R.array.default_blacklist))));
        if(Preferences.getBoolean(this, "first_run", true) && Util.isTaskerInstalled(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Showing dialog telling the user that this app supports Tasker");
            new AlertDialog.Builder(this,ThemeHandler.getDialogTheme(this)).setTitle(R.string.tasker_support).setMessage(R.string.app_supports_tasker_text).setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).show();
            LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
        }
        int random = new Random().nextInt(100), launches = Preferences.getInteger(this, "launches", 0);
        Preferences.put(this, "launches", launches+1);
        if(!Preferences.getBoolean(this, "first_run",true) && !Preferences.getBoolean(this, "rated",false) && random <= (launches >= 3 ? 8 : 3)){
            LogFactory.writeMessage(this, LOG_TAG, "Showing dialog requesting rating");
            new AlertDialog.Builder(this,ThemeHandler.getDialogTheme(this)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    rateApp();
                }
            }).setNegativeButton(R.string.dont_ask_again, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Preferences.put(MainActivity.this, "rated",true);
                    dialog.cancel();
                }
            }).setNeutralButton(R.string.not_now, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            }).setMessage(R.string.rate_request_text).setTitle(R.string.rate).show();
            LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
        }
        Util.updateTiles(this);
        View cardView = getLayoutInflater().inflate(R.layout.main_cardview, null, false);
        final TextView text = cardView.findViewById(R.id.text);
        final Switch button = cardView.findViewById(R.id.cardview_switch);
        if(PreferencesAccessor.isEverythingDisabled(this)){
            button.setChecked(true);
            text.setText(R.string.cardview_text_disabled);
        }
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                text.setText(b ? R.string.cardview_text_disabled : R.string.cardview_text);
                Preferences.put(MainActivity.this, "everything_disabled", b);
                if(Util.isServiceRunning(MainActivity.this))startService(DNSVpnService.getDestroyIntent(MainActivity.this));
            }
        });
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                button.toggle();
            }
        });
        setCardView(cardView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getCurrentFragment() == settingsFragment){
            getMenuInflater().inflate(R.menu.menu_settings, menu);

            SearchManager searchManager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
            searchView.setOnQueryTextListener(settingsFragment);
            return true;
        }else return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        if(dialog1 != null && dialog1.isShowing())dialog1.cancel();
        if(defaultDnsDialog != null && defaultDnsDialog.isShowing())defaultDnsDialog.cancel();
        if(importFinishedReceiver != null)unregisterReceiver(importFinishedReceiver);
        unregisterReceiver(shortcutReceiver);
        super.onDestroy();
        currentContext = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch(keyCode){
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if(currentFragment() instanceof MainFragment){
                    handled = true;
                    ((MainFragment)currentFragment()).toggleVPN();
                }break;
            case KeyEvent.KEYCODE_DPAD_DOWN:case KeyEvent.KEYCODE_PAGE_DOWN:case KeyEvent.KEYCODE_DPAD_UP:case KeyEvent.KEYCODE_PAGE_UP:
                if(currentFragment() instanceof MainFragment){
                    if(((MainFragment)currentFragment()).toggleCurrentInputFocus())handled = true;
                }
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    @NonNull
    @Override
    public DrawerItem getDefaultItem() {
        return defaultDrawerItem;
    }

    @Override
    public void onItemClicked(DrawerItem item, boolean handle) {
    }

    public Fragment currentFragment(){
        return getCurrentFragment();
    }

    @Override
    public List<DrawerItem> createDrawerItems() {
        DrawerItemCreator itemCreator = new DrawerItemCreator(this);
        itemCreator.createItemAndContinue(R.string.nav_title_main);
        itemCreator.createItemAndContinue(R.string.nav_title_dns, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_home)), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment(@Nullable Bundle arguments) {
                return mainFragment=new MainFragment();
            }
        }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
            @Override
            public void access(DrawerItem item) {
                defaultDrawerItem = item;
                item.setRecreateFragmentOnConfigChange(true);
            }
        });
        itemCreator.createItemAndContinue(R.string.settings, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_settings)), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment(@Nullable Bundle arguments) {
                settingsFragment = new SettingsFragment();
                if(arguments != null)settingsFragment.setArguments(arguments);
                return settingsFragment;
            }
        }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
            @Override
            public void access(DrawerItem item) {
                settingsDrawerItem = item;
                item.setInvalidateActivityMenu(true);
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_dns_query, setDrawableColor(DesignUtil.getDrawable(this, android.R.drawable.ic_menu_search)), new DrawerItem.FragmentCreator() {
            @Override
            public Fragment getFragment(@Nullable Bundle arguments) {
                return new DnsQueryFragment();
            }
        }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
            @Override
            public void access(DrawerItem item) {
                item.setInvalidateActivityMenu(true);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            itemCreator.createItemAndContinue(R.string.nav_title_current_networks, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_network_check)), new DrawerItem.FragmentCreator() {
                @NonNull
                @Override
                public Fragment getFragment(@Nullable Bundle arguments) {
                    return new CurrentNetworksFragment();
                }
            });
        }
        if(PreferencesAccessor.isAdvancedModeEnabled(this)){
            itemCreator.createItemAndContinue(R.string.nav_title_advanced);
            itemCreator.createItemAndContinue(R.string.title_advanced_settings, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_settings)), new DrawerItem.ClickListener() {
                @Override
                public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                    startActivity(new Intent(MainActivity.this, AdvancedSettingsActivity.class));
                    return false;
                }

                @Override
                public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                    return false;
                }
            });
            if(PreferencesAccessor.areRulesEnabled(this)){
                itemCreator.createItemAndContinue(R.string.nav_title_rules, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_list_bullet_point)), new DrawerItem.FragmentCreator() {
                    @Override
                    public Fragment getFragment(@Nullable Bundle arguments) {
                        return new RulesFragment();
                    }
                });
            }
            if(PreferencesAccessor.isQueryLoggingEnabled(this)){
                itemCreator.createItemAndContinue(R.string.nav_title_query_log, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_timelapse)), new DrawerItem.FragmentCreator() {
                    @Override
                    public Fragment getFragment(@Nullable Bundle arguments) {
                        return new QueryLogFragment();
                    }
                }).accessLastItemAndContinue(new DrawerItemCreator.ItemAccessor() {
                    @Override
                    public void access(DrawerItem item) {
                        item.setRecreateFragmentOnConfigChange(true);
                    }
                });
            }
        }
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                return false;
            }
        });
        if(Util.isTaskerInstalled(this)){
            itemCreator.createItemAndContinue(R.string.tasker_support, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_thumb_up)), new DrawerItem.ClickListener() {
                @Override
                public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                    return false;
                }

                @Override
                public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

                @Override
                public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                    return false;
                }
            });
        }
        itemCreator.createItemAndContinue(R.string.nav_title_pin_protection, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_action_key)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                dialog1 = new AlertDialog.Builder(MainActivity.this).setTitle(item.getTitle()).setNegativeButton(R.string.close, null)
                        .setPositiveButton("pos", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                openSettingsAndScrollToKey("pin_category");
                            }
                        }).setMessage(R.string.feature_pin_protection).create();
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_more, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_ellipsis)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                return false;
            }

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_importexport);
        itemCreator.createItemAndContinue(R.string.title_import_settings, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_action_import)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                if(!PermissionsUtil.canReadExternalStorage(MainActivity.this)){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_IMPORT_SETTINGS);
                }else{
                    importSettings();
                }
                return false;
            }

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.title_export_settings,setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_action_export)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                if(!PermissionsUtil.canWriteExternalStorage(MainActivity.this)){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_EXPORT_SETTINGS);
                }else{
                    new ExportSettingsDialog(MainActivity.this);
                }
                return false;
            }

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                return false;
            }
        });
        itemCreator.createItemAndContinue(R.string.nav_title_libraries, setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_library_books)), new DrawerItem.ClickListener() {
            @Override
            public boolean onClick(DrawerItem item, NavigationDrawerActivity drawerActivity, @Nullable Bundle arguments) {
                String licenseText = getString(R.string.dialog_libraries_text) + "\n\n- - - - - - - - - - - -\ndnsjava by Brian Wellington - http://www.xbill.org/dnsjava/\n\n" + getString(R.string.license_bsd_2).replace("[yearrange]", "1998-2011").replace("[author]", "Brian Wellington");
                licenseText += "\n\n- - - - - - - - - - - -\nfirebase-jobdispatcher-android by Google\n\nAvailable under the [1]Apache License 2.0[2]";
                licenseText += "\n\n- - - - - - - - - - - -\npcap4j by Kaito Yamada\n\nAvailable under the [3]MIT License[4]";
                licenseText += "\n\n- - - - - - - - - - - -\nMiniDNS by Measite\n\nAvailable under the [5]Apache License 2.0[6]";
                licenseText += "\n\n- - - - - - - - - - - -\nMaterial icon pack by Google\n\nAvailable under the [7]Apache License 2.0[8]";
                ClickableSpan span = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(MainActivity.this).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show();
                    }
                }, span2 = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        String text = getString(R.string.mit_license);
                        text = text.replace("[name]", "Pcap4J");
                        text = text.replace("[author]", "Pcap4J.org");
                        text = text.replace("[yearrange]", "2011-2017");
                        new AlertDialog.Builder(MainActivity.this).setTitle("MIT License").setPositiveButton(R.string.close, null).setMessage(text).show();
                    }
                }, span3 = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(MainActivity.this).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show();
                    }
                }, span4 = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        new AlertDialog.Builder(MainActivity.this).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show();
                    }
                };
                SpannableString spannable = new SpannableString(licenseText.replaceAll("\\[.]",""));
                spannable.setSpan(span3, licenseText.indexOf("[1]"), licenseText.indexOf("[2]")-3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(span2, licenseText.indexOf("[3]")-6, licenseText.indexOf("[4]")-9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(span, licenseText.indexOf("[5]")-12, licenseText.indexOf("[6]")-15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(span4, licenseText.indexOf("[7]")-18, licenseText.indexOf("[8]")-21, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                dialog1 = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.nav_title_libraries).setNegativeButton(R.string.close, null)
                        .setMessage(spannable).show();
                ((TextView)dialog1.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return false;
            }

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
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

            @Override
            public boolean onLongClick(DrawerItem item, NavigationDrawerActivity drawerActivity) {
                new AlertDialog.Builder(MainActivity.this).setMessage(R.string.easter_egg).setTitle("(╯°□°）╯︵ ┻━┻")
                        .setPositiveButton("Okay :(", null).show();
                return true;
            }
        });
        return itemCreator.getDrawerItems();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        System.out.println("PERMISSION RESULT; " + (requestCode == REQUEST_PERMISSION_EXPORT_SETTINGS) + "   " + grantResults[0]);
        if(requestCode == REQUEST_PERMISSION_EXPORT_SETTINGS && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            new ExportSettingsDialog(this);
        }else if(requestCode == REQUEST_PERMISSION_IMPORT_SETTINGS && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            importSettings();
        }
    }

    private void importSettings(){
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Importing Setting. Showing chooser dialog.");
        new FileChooserDialog(this, false, FileChooserDialog.SelectionMode.FILE).setFileListener(new FileChooserDialog.FileSelectedListener() {
            @Override
            public void fileSelected(File file, FileChooserDialog.SelectionMode selectionMode) {
                LogFactory.writeMessage(MainActivity.this, new String[]{LOG_TAG,"[IMPORTSETTINGS]"}, "User choose File " + file);
                LogFactory.writeMessage(MainActivity.this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Finishing Activity");
                finish();
                LogFactory.writeMessage(MainActivity.this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Starting import (Opening SettingsImportActivity");
                SettingsImportActivity.importFromFile(MainActivity.this, file);
            }

            @Override
            public void multipleFilesSelected(File... files) {

            }
        }).showDialog();
        LogFactory.writeMessage(this, new String[]{LOG_TAG, "[IMPORTSETTINGS]"}, "Dialog is now showing");

    }

    private void openSettingsAndScrollToKey(String key){
        if(getCurrentFragment() instanceof SettingsFragment)((SettingsFragment)getCurrentFragment()).scrollToPreference(key);
        else{
            Bundle arguments = new Bundle();
            arguments.putString(SettingsFragment.ARGUMENT_SCROLL_TO_SETTING, key);
            clickItem(settingsDrawerItem, arguments);
        }
    }

    private Drawable setDrawableColor(Drawable drawable){
        DrawableCompat.setTint(drawable.mutate(), navDrawableColor);
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

    @Override
    public boolean useItemBackStack() {
        return true;
    }

    @Override
    public int maxBackStackRecursion() {
        return 0;
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
        defaultDnsDialog = new DefaultDNSDialog(this, ThemeHandler.getDialogTheme(this), new DefaultDNSDialog.OnProviderSelectedListener() {
            @Override
            public void onProviderSelected(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6) {
                boolean port = PreferencesAccessor.areCustomPortsEnabled(MainActivity.this);
                if(mainFragment.settingV6){
                    mainFragment.dns1.setText(dns1V6.toString(port));
                    mainFragment.dns2.setText(dns2V6.toString(port));
                    boolean ipEnabled = PreferencesAccessor.isIPv4Enabled(MainActivity.this);
                    if(ipEnabled)PreferencesAccessor.Type.DNS1.saveDNSPair(MainActivity.this, dns1);
                    if(ipEnabled)PreferencesAccessor.Type.DNS2.saveDNSPair(MainActivity.this, dns2);
                }else{
                    mainFragment.dns1.setText(dns1.toString(port));
                    mainFragment.dns2.setText(dns2.toString(port));
                    boolean ipEnabled = PreferencesAccessor.isIPv6Enabled(MainActivity.this);
                    if(ipEnabled)PreferencesAccessor.Type.DNS1_V6.saveDNSPair(MainActivity.this, dns1V6);
                    if(ipEnabled)PreferencesAccessor.Type.DNS2_V6.saveDNSPair(MainActivity.this, dns2V6);
                }
                if(Util.isServiceRunning(MainActivity.this))
                    MainActivity.this.startService(DNSVpnService.getUpdateServersIntent(MainActivity.this, true, false));
            }
        });
        defaultDnsDialog.show();
        LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!startedActivity && (PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP)))finish();
    }

    @Override
    public void startActivity(Intent intent) {
        if((intent.getAction() != null && intent.getAction().equals(Intent.ACTION_CHOOSER)) || (intent.getComponent() != null && intent.getComponent().getPackageName().equals("com.frostnerd.dnschanger"))){
            startedActivity = true;
        }
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if((intent.getAction() != null && intent.getAction().equals(Intent.ACTION_CHOOSER)) || (intent.getComponent() != null && intent.getComponent().getPackageName().equals("com.frostnerd.dnschanger"))){
            startedActivity = true;
        }
        super.startActivityForResult(intent, requestCode);
    }


    @Override
    public void importStarted(int combinedLines) {
        final LoadingDialog loadingDialog = new LoadingDialog(this, ThemeHandler.getDialogTheme(this),
                getString(R.string.importing_x_rules).replace("[x]", combinedLines + ""),
                getString(R.string.info_importing_rules_app_unusable));
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.show();
        registerReceiver(importFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadingDialog.dismiss();
                unregisterReceiver(this);
                importFinishedReceiver = null;
            }
        }, new IntentFilter(RuleImportService.BROADCAST_IMPORT_FINISHED));
    }
}
