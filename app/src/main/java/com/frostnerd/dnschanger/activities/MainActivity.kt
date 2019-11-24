package com.frostnerd.dnschanger.activities

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.KeyEvent
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.collection.ArraySet
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.frostnerd.design.dialogs.FileChooserDialog
import com.frostnerd.design.dialogs.LoadingDialog
import com.frostnerd.dnschanger.BuildConfig
import com.frostnerd.dnschanger.LogFactory
import com.frostnerd.dnschanger.R
import com.frostnerd.dnschanger.database.entities.IPPortPair
import com.frostnerd.dnschanger.dialogs.DNSEntryListDialog
import com.frostnerd.dnschanger.dialogs.ExportSettingsDialog
import com.frostnerd.dnschanger.fragments.*
import com.frostnerd.dnschanger.services.DNSVpnService
import com.frostnerd.dnschanger.services.RuleImportService
import com.frostnerd.dnschanger.tasker.ConfigureActivity
import com.frostnerd.dnschanger.util.*
import com.frostnerd.general.Utils
import com.frostnerd.general.permissions.PermissionsUtil
import com.frostnerd.navigationdraweractivity.NavigationDrawerActivity
import com.frostnerd.navigationdraweractivity.StyleOptions
import com.frostnerd.navigationdraweractivity.items.DrawerItem
import com.frostnerd.navigationdraweractivity.items.createMenu
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

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
class MainActivity : NavigationDrawerActivity(), RuleImport.ImportStartedListener {
    private val REQUEST_PERMISSION_IMPORT_SETTINGS = 131
    private val REQUEST_PERMISSION_EXPORT_SETTINGS = 130
    private val LOG_TAG = "[MainActivity]"
    override val drawerOverActionBar: Boolean = true
    private var startedActivity = false
    @ColorInt
    private var textColor: Int = 0
    @ColorInt
    private var backgroundColor: Int = 0
    @ColorInt
    private var navDrawableColor: Int = 0
    private val shortcutReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val snackbar = Snackbar.make(findViewById(R.id.drawerLayout), R.string.shortcut_created, Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction(R.string.show) {
                snackbar.dismiss()
                Utils.goToLauncher(this@MainActivity)
            }
            snackbar.show()
        }
    }
    private var importingRules = false
    private var importFinishedReceiver:BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeHandler.getAppTheme(this))
        backgroundColor = ThemeHandler.resolveThemeAttribute(theme, android.R.attr.colorBackground)
        textColor = ThemeHandler.resolveThemeAttribute(theme, android.R.attr.textColor)
        navDrawableColor = ThemeHandler.resolveThemeAttribute(theme, R.attr.navDrawableColor)
        super.onCreate(savedInstanceState)
        Util.updateAppShortcuts(this)
        Util.runBackgroundConnectivityCheck(this, true)

        val preferences = Preferences.getInstance(this)
        if (preferences.getBoolean("first_run", true)) preferences.put("excluded_apps", ArraySet(Arrays.asList(*resources.getStringArray(R.array.default_blacklist))))
        if (preferences.getBoolean("first_run", true) && Util.isTaskerInstalled(this)) {
            LogFactory.writeMessage(this, LOG_TAG, "Showing dialog telling the user that this app supports Tasker")
            AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setTitle(R.string.tasker_support).setMessage(R.string.app_supports_tasker_text).setPositiveButton(R.string.got_it) { dialog, which -> dialog.cancel() }.show()
            LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown")
        }
        val random = Random().nextInt(100)
        val launches = preferences.getInteger("launches", 0)
        preferences.put("launches", launches + 1)
        if (launches >= 5 && !preferences.getBoolean("first_run", true) &&
                !preferences.getBoolean("rated", false) && random <= 16) {
            LogFactory.writeMessage(this, LOG_TAG, "Showing dialog reqesting rating")
            AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this)).setPositiveButton(R.string.ok) { dialog, which -> rateApp() }.setNegativeButton(R.string.dont_ask_again) { dialog, which ->
                preferences.put("rated", true)
                dialog.cancel()
            }.setNeutralButton(R.string.not_now) { dialog, which -> dialog.cancel() }.setMessage(R.string.rate_request_text).setTitle(R.string.rate).show()
            LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown")
        }
        Util.updateTiles(this)
        setCardView { viewParent, suggestedHeight ->
            val cardView = layoutInflater.inflate(R.layout.main_cardview, viewParent, false)
            val text = cardView.findViewById<TextView>(R.id.text)
            val button = cardView.findViewById<Switch>(R.id.cardview_switch)
            if (PreferencesAccessor.isEverythingDisabled(this)) {
                button.setChecked(true)
                text.setText(R.string.cardview_text_disabled)
            }
            button.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton, b ->
                LogFactory.writeMessage(this, arrayOf(LOG_TAG, "[DISABLE-EVERYTHING]"), "The DisableEverything switch was clicked and changed to $b")
                text.setText(if (b) R.string.cardview_text_disabled else R.string.cardview_text)
                preferences.put("everything_disabled", b)
                if (Util.isServiceRunning(this)) {
                    LogFactory.writeMessage(this, arrayOf(LOG_TAG, "[DISABLE-EVERYTHING]"), "Service is running. Destroying...")
                    startService(DNSVpnService.getDestroyIntent(this))
                }
            })
            cardView.setOnClickListener { button.toggle() }
            cardView
        }
        preferences.put("first_run", false)
    }

    override fun onResume() {
        super.onResume()
        startedActivity = false
        // Receiver is not unregistered in onPause() because the app is in the background when a shortcut
        // is created
        registerReceiver(shortcutReceiver, IntentFilter(Util.BROADCAST_SHORTCUT_CREATED))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(shortcutReceiver)
        if(importFinishedReceiver != null) unregisterReceiver(importFinishedReceiver)
    }

    override fun onStop() {
        super.onStop()
        if (!startedActivity && PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.APP)) finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_EXPORT_SETTINGS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ExportSettingsDialog(this)
        } else if (requestCode == REQUEST_PERMISSION_IMPORT_SETTINGS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            importSettings()
        }
    }

    override fun startActivity(intent: Intent) {
        updatePinState(intent)
        super.startActivity(intent)
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        updatePinState(intent)
        super.startActivityForResult(intent, requestCode)
    }

    override fun startActivityFromFragment(fragment: android.app.Fragment, intent: Intent, requestCode: Int) {
        updatePinState(intent)
        super.startActivityFromFragment(fragment, intent, requestCode)
    }

    override fun startActivityFromFragment(fragment: android.app.Fragment, intent: Intent, requestCode: Int, options: Bundle?) {
        updatePinState(intent)
        super.startActivityFromFragment(fragment, intent, requestCode, options)
    }

    override fun startActivityFromFragment(fragment: Fragment, intent: Intent, requestCode: Int) {
        updatePinState(intent)
        super.startActivityFromFragment(fragment, intent, requestCode)
    }

    override fun startActivityFromFragment(fragment: Fragment, intent: Intent, requestCode: Int, options: Bundle?) {
        updatePinState(intent)
        super.startActivityFromFragment(fragment, intent, requestCode, options)
    }


    private fun updatePinState(intent: Intent) {
        if ((intent.action == Intent.ACTION_CHOOSER) || intent.component?.packageName == "com.frostnerd.dnschanger") {
            startedActivity = true
        }
    }

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return Preferences.getInstance(this)
    }

    private fun openSettingsAndScrollToKey(key: String) {
        if (currentFragment is SettingsFragment) (currentFragment as SettingsFragment).scrollToPreference(key)
        else {
            clickItem(drawerItems[2], Bundle().apply {
                putString(SettingsFragment.ARGUMENT_SCROLL_TO_SETTING, key)
            })
        }
    }

    fun openDefaultDNSDialog(v: View) {
        LogFactory.writeMessage(this, LOG_TAG, "Opening DNSEntryListDialog")
        val mainFragment = currentFragment as MainFragment
        DNSEntryListDialog(this, ThemeHandler.getDialogTheme(this), DNSEntryListDialog.OnProviderSelectedListener { name, dns1, dns2, dns1V6, dns2V6 ->
            val port = PreferencesAccessor.areCustomPortsEnabled(this@MainActivity)
            if (mainFragment.settingV6) {
                mainFragment.dns1.setText(dns1V6.toString(port))
                mainFragment.dns2.setText(dns2V6.toString(port))
                val ipEnabled = PreferencesAccessor.isIPv4Enabled(this@MainActivity)
                if (ipEnabled) PreferencesAccessor.Type.DNS1.saveDNSPair(this@MainActivity, dns1)
                if (ipEnabled) PreferencesAccessor.Type.DNS2.saveDNSPair(this@MainActivity, dns2)
            } else {
                mainFragment.dns1.setText(dns1.toString(port))
                mainFragment.dns2.setText(dns2.toString(port))
                val ipEnabled = PreferencesAccessor.isIPv6Enabled(this@MainActivity)
                if (ipEnabled) PreferencesAccessor.Type.DNS1_V6.saveDNSPair(this@MainActivity, dns1V6)
                if (ipEnabled) PreferencesAccessor.Type.DNS2_V6.saveDNSPair(this@MainActivity, dns2V6)
            }
            applyDNSServersInstant()
        }).show()
        LogFactory.writeMessage(this, LOG_TAG, "Dialog is now being shown")
    }

    private fun applyDNSServersInstant() {
        if (Util.isServiceRunning(this@MainActivity)) {
            if (PreferencesAccessor.checkConnectivityOnStart(this)) {
                if (currentFragment is MainFragment) {
                    val dialog = LoadingDialog(this, R.string.checking_connectivity, R.string.dialog_connectivity_description)
                    dialog.show()
                    (currentFragment as MainFragment).checkDNSReachability(object : MainFragment.DNSReachabilityCallback() {
                        override fun checkFinished(unreachable: List<IPPortPair>, reachable: List<IPPortPair>) {
                            dialog.dismiss()
                            if (unreachable.size == 0) {
                                this@MainActivity.runOnUiThread(Runnable { this@MainActivity.startService(DNSVpnService.getUpdateServersIntent(this@MainActivity, true, false)) })
                            } else {
                                var _text = getString(R.string.no_connectivity_warning_text)
                                val builder = StringBuilder()
                                _text = _text.replace("[x]", (unreachable.size + reachable.size).toString() + "")
                                _text = _text.replace("[y]", unreachable.size.toString() + "")
                                val customPorts = PreferencesAccessor.areCustomPortsEnabled(this@MainActivity)
                                for (p in unreachable) if (p != null) builder.append("- ").append(p.formatForTextfield(customPorts)).append("\n")
                                _text = _text.replace("[servers]", builder.toString())
                                val text = _text
                                this@MainActivity.runOnUiThread(Runnable {
                                    AlertDialog.Builder(this@MainActivity, ThemeHandler.getDialogTheme(this@MainActivity))
                                            .setTitle(R.string.warning).setCancelable(true).setPositiveButton(R.string.start) { dialogInterface, i -> this@MainActivity.startService(DNSVpnService.getUpdateServersIntent(this@MainActivity, true, false)) }.setNegativeButton(R.string.cancel, null).setMessage(text).show()
                                })
                            }
                        }
                    })
                } else {
                    this.startService(DNSVpnService.getUpdateServersIntent(this@MainActivity, true, false))
                }
            } else {
                this.startService(DNSVpnService.getUpdateServersIntent(this@MainActivity, true, false))
            }
        }
    }

    private fun importSettings() {
        LogFactory.writeMessage(this, arrayOf(LOG_TAG, "[IMPORTSETTINGS]"), "Importing Setting. Showing chooser dialog.")
        FileChooserDialog(this, false, FileChooserDialog.SelectionMode.FILE).setFileListener(object : FileChooserDialog.FileSelectedListener {
            override fun fileSelected(file: File, selectionMode: FileChooserDialog.SelectionMode) {
                LogFactory.writeMessage(this@MainActivity, arrayOf(LOG_TAG, "[IMPORTSETTINGS]"), "User choose File $file")
                LogFactory.writeMessage(this@MainActivity, arrayOf(LOG_TAG, "[IMPORTSETTINGS]"), "Finishing Activity")
                finish()
                LogFactory.writeMessage(this@MainActivity, arrayOf(LOG_TAG, "[IMPORTSETTINGS]"), "Starting import (Opening SettingsImportActivity")
                SettingsImportActivity.importFromFile(this@MainActivity, file)
            }

            override fun multipleFilesSelected(vararg files: File) {

            }
        }).showDialog()
        LogFactory.writeMessage(this, arrayOf(LOG_TAG, "[IMPORTSETTINGS]"), "Dialog is now showing")
    }

    override fun createDrawerItems(): MutableList<DrawerItem> {
        return createMenu {
            header(getString(R.string.nav_title_main))
            fragmentItem(getString(R.string.nav_title_dns),
                    iconLeft = getDrawable(R.drawable.ic_home),
                    recreateFragmentOnConfigChange = true,
                    fragmentCreator = {
                        MainFragment()
                    }
            )
            fragmentItem(getString(R.string.settings),
                    iconLeft = getDrawable(R.drawable.ic_settings),
                    shouldInvalidateOptionsMenu = true,
                    fragmentCreator = {
                        SettingsFragment().apply {
                            if (it != null) arguments = it
                        }
                    })
            fragmentItem(getString(R.string.nav_title_dns_query),
                    iconLeft = getDrawable(android.R.drawable.ic_menu_search),
                    fragmentCreator = {
                        DnsQueryFragment()
                    })
            fragmentItem(getString(R.string.nav_title_current_networks),
                    iconLeft = getDrawable(R.drawable.ic_network_check),
                    shouldInvalidateOptionsMenu = true,
                    fragmentCreator = {
                        CurrentNetworksFragment()
                    })
            if (PreferencesAccessor.isAdvancedModeEnabled(this@MainActivity)) {
                divider()
                header(getString(R.string.nav_title_advanced))
                clickableItem(getString(R.string.title_advanced_settings),
                        iconLeft = getDrawable(R.drawable.ic_settings),
                        onSimpleClick = { item, drawerActivity, arguments ->
                            startActivity(Intent(this@MainActivity, AdvancedSettingsActivity::class.java))
                            false
                        },
                        onLongClick = null)
                if (PreferencesAccessor.areRulesEnabled(this@MainActivity)) {
                    fragmentItem(getString(R.string.nav_title_rules),
                            iconLeft = getDrawable(R.drawable.ic_list_bullet_point),
                            fragmentCreator = {
                                RulesFragment()
                            })
                }
                if (PreferencesAccessor.isQueryLoggingEnabled(this@MainActivity)) {
                    fragmentItem(getString(R.string.nav_title_query_log),
                            iconLeft = getDrawable(R.drawable.ic_timelapse),
                            fragmentCreator = {
                                QueryLogFragment()
                            }, recreateFragmentOnConfigChange = true)
                }
            }
            divider()
            header(getString(R.string.nav_title_learn))
            clickableItem(getString(R.string.nav_title_how_does_it_work),
                    iconLeft = getDrawable(R.drawable.ic_wrench),
                    onSimpleClick = { _, _, _ ->
                        AlertDialog.Builder(this@MainActivity, ThemeHandler.getDialogTheme(this@MainActivity)).setTitle(R.string.nav_title_how_does_it_work)
                                .setMessage(R.string.info_text_how_does_it_work).setNeutralButton(R.string.close) { dialogInterface, i -> dialogInterface.dismiss() }.show()
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.nav_title_what_is_dns),
                    iconLeft = getDrawable(R.drawable.ic_help),
                    onSimpleClick = { _, _, _ ->
                        LogFactory.writeMessage(this@MainActivity, LOG_TAG, "Opening Dialog with info about DNS")
                        AlertDialog.Builder(this@MainActivity, ThemeHandler.getDialogTheme(this@MainActivity)).setTitle(R.string.info_dns_button).setMessage(R.string.info_text_dns).setCancelable(true).setNeutralButton(R.string.ok) { dialog, which -> dialog.cancel() }.show()
                        LogFactory.writeMessage(this@MainActivity, LOG_TAG, "Dialog is now being shown")
                        false
                    }, onLongClick = null)
            divider()
            header(getString(R.string.nav_title_features))
            clickableItem(getString(R.string.shortcuts),
                    iconLeft = getDrawable(R.drawable.ic_open_in_new),
                    onSimpleClick = { _, _, _ ->
                        val dialog = AlertDialog.Builder(this@MainActivity).setTitle(R.string.shortcuts).setNegativeButton(R.string.close, null)
                                .setPositiveButton("pos") { dialogInterface, i -> openSettingsAndScrollToKey("shortcut_category") }
                                .setNeutralButton(R.string.create_one) { dialogInterface, cnt ->
                                    val i: Intent = Intent(this@MainActivity, ConfigureActivity::class.java).putExtra("creatingShortcut", true)
                                    LogFactory.writeMessage(this@MainActivity, LOG_TAG, "User wants to create a shortcut", i)
                                    startActivityForResult(i, 1)
                                }.setMessage(R.string.feature_shortcuts).create()
                        dialog.setOnShowListener(DialogInterface.OnShowListener {
                            val button = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                            button.setText("")
                            val gear = setDrawableColor(resources.getDrawable(R.drawable.ic_settings))
                            button.setCompoundDrawablesWithIntrinsicBounds(gear, null, null, null)
                        })
                        dialog.show()
                        false
                    }, onLongClick = null)
            if (Util.isTaskerInstalled(this@MainActivity)) {
                clickableItem(getString(R.string.tasker_support),
                        iconLeft = getDrawable(R.drawable.ic_thumb_up),
                        onSimpleClick = null,
                        onLongClick = null)
            }
            clickableItem(getString(R.string.tasker_support),
                    iconLeft = getDrawable(R.drawable.ic_thumb_up),
                    onSimpleClick = { _, _, _ ->
                        AlertDialog.Builder(this@MainActivity).setTitle(R.string.nav_title_tiles).setMessage(R.string.feature_tiles)
                                .setNeutralButton(R.string.close, null).show()
                        false
                    },
                    onLongClick = null)
            clickableItem(getString(R.string.nav_title_pin_protection),
                    iconLeft = getDrawable(R.drawable.ic_action_key),
                    onSimpleClick = { _, _, _ ->
                        val dialog = AlertDialog.Builder(this@MainActivity).setTitle(getString(R.string.nav_title_pin_protection)).setNegativeButton(R.string.close, null)
                                .setPositiveButton("pos", DialogInterface.OnClickListener { dialogInterface, i -> openSettingsAndScrollToKey("pin_category") }).setMessage(R.string.feature_pin_protection).create()
                        dialog.setOnShowListener(DialogInterface.OnShowListener {
                            val button = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                            button.setText("")
                            val gear = setDrawableColor(resources.getDrawable(R.drawable.ic_settings))
                            button.setCompoundDrawablesWithIntrinsicBounds(gear, null, null, null)
                        })
                        dialog.show()
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.nav_title_more),
                    iconLeft = getDrawable(R.drawable.ic_ellipsis),
                    onSimpleClick = null,
                    onLongClick = null)
            divider()
            header(getString(R.string.nav_title_importexport))
            clickableItem(getString(R.string.title_import_settings),
                    iconLeft = getDrawable(R.drawable.ic_action_import),
                    onSimpleClick = { _, _, _ ->
                        if (!PermissionsUtil.canReadExternalStorage(this@MainActivity)) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_IMPORT_SETTINGS)
                        } else {
                            importSettings()
                        }
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.title_export_settings),
                    iconLeft = getDrawable(R.drawable.ic_action_export),
                    onSimpleClick = { _, _, _ ->
                        if (!PermissionsUtil.canWriteExternalStorage(this@MainActivity)) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_EXPORT_SETTINGS)
                        } else {
                            ExportSettingsDialog(this@MainActivity)
                        }
                        false
                    }, onLongClick = null)
            divider()
            header(getString(R.string.app_name))
            clickableItem(getString(R.string.rate),
                    iconLeft = getDrawable(R.drawable.ic_star),
                    onSimpleClick = { _, _, _ ->
                        rateApp()
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.title_share_app),
                    iconLeft = getDrawable(R.drawable.ic_share),
                    onSimpleClick = { _, _, _ ->
                        val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                        sharingIntent.type = "text/plain"
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.app_share_text))
                        LogFactory.writeMessage(this@MainActivity, LOG_TAG, "Showing chooser for share", sharingIntent)
                        startActivity(Intent.createChooser(sharingIntent, resources.getString(R.string.share_using)))
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.contact_developer),
                    iconLeft = getDrawable(R.drawable.ic_person),
                    onSimpleClick = { _, _, _ ->
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", "support@frostnerd.com", null))
                        val body = "\n\n\n\n\n\n\nSystem:\nApp version: " + BuildConfig.VERSION_CODE + " (" + BuildConfig.VERSION_NAME + ")\n" +
                                "Android: " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + " - " + Build.VERSION.CODENAME + ")"
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, "support@frostnerd.com")
                        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
                        LogFactory.writeMessage(this@MainActivity, LOG_TAG, "Now showing chooser for contacting dev", emailIntent)
                        startActivity(Intent.createChooser(emailIntent, getString(R.string.contact_developer)))
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.nav_title_libraries),
                    iconLeft = getDrawable(R.drawable.ic_library_books),
                    onSimpleClick = { _, _, _ ->
                        var licenseText = getString(R.string.dialog_libraries_text) + "\n\n- - - - - - - - - - - -\ndnsjava by Brian Wellington - http://www.xbill.org/dnsjava/\n\n" + getString(R.string.license_bsd_2).replace("[yearrange]", "1998-2011").replace("[author]", "Brian Wellington")
                        licenseText += "\n\n- - - - - - - - - - - -\nfirebase-jobdispatcher-android by Google\n\nAvailable under the [1]Apache License 2.0[2]"
                        licenseText += "\n\n- - - - - - - - - - - -\npcap4j by Kaito Yamada\n\nAvailable under the [3]MIT License[4]"
                        licenseText += "\n\n- - - - - - - - - - - -\nMiniDNS by Measite\n\nAvailable under the [5]Apache License 2.0[6]"
                        licenseText += "\n\n- - - - - - - - - - - -\nMaterial icon pack by Google\n\nAvailable under the [7]Apache License 2.0[8]"
                        licenseText += "\n\n- - - - - - - - - - - -\nGson by google\n\nAvailable under the [9]Apache License 2.0[a]"
                        val span = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                AlertDialog.Builder(this@MainActivity).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show()
                            }
                        }
                        val span2 = object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                var text = getString(R.string.mit_license)
                                text = text.replace("[name]", "Pcap4J")
                                text = text.replace("[author]", "Pcap4J.org")
                                text = text.replace("[yearrange]", "2011-2017")
                                AlertDialog.Builder(this@MainActivity).setTitle("MIT License").setPositiveButton(R.string.close, null).setMessage(text).show()
                            }
                        }
                        val span3 = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                AlertDialog.Builder(this@MainActivity).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show()
                            }
                        }
                        val span4 = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                AlertDialog.Builder(this@MainActivity).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show()
                            }
                        }
                        val span5 = object : ClickableSpan() {
                            override fun onClick(view: View) {
                                AlertDialog.Builder(this@MainActivity).setTitle("Apache License 2.0").setPositiveButton(R.string.close, null).setMessage(R.string.license_apache_2).show()
                            }
                        }
                        val spannable = SpannableString(licenseText.replace("\\[.]".toRegex(), ""))
                        spannable.setSpan(span3, licenseText.indexOf("[1]"), licenseText.indexOf("[2]") - 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(span2, licenseText.indexOf("[3]") - 6, licenseText.indexOf("[4]") - 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(span, licenseText.indexOf("[5]") - 12, licenseText.indexOf("[6]") - 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(span4, licenseText.indexOf("[7]") - 18, licenseText.indexOf("[8]") - 21, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(span5, licenseText.indexOf("[9]") - 24, licenseText.indexOf("[a]") - 27, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        val dialog = AlertDialog.Builder(this@MainActivity).setTitle(R.string.nav_title_libraries).setNegativeButton(R.string.close, null)
                                .setMessage(spannable).show()
                        dialog.findViewById<TextView>(android.R.id.message)!!.movementMethod = LinkMovementMethod.getInstance()
                        false
                    }, onLongClick = null)
            clickableItem(getString(R.string.title_about),
                    iconLeft = getDrawable(R.drawable.ic_info),
                    onSimpleClick = { _, _, _ ->
                        val text = getString(R.string.about_text).replace("[[version]]", BuildConfig.VERSION_NAME).replace("[[build]]", BuildConfig.VERSION_CODE.toString() + "")
                        AlertDialog.Builder(this@MainActivity).setTitle(R.string.title_about).setMessage(text)
                                .setNegativeButton(R.string.close) { dialogInterface, i -> dialogInterface.dismiss() }.show()
                        false
                    }, onLongClick = { _, _ ->
                AlertDialog.Builder(this@MainActivity).setMessage(R.string.easter_egg).setTitle("(╯°□°）╯︵ ┻━┻")
                        .setPositiveButton("Okay :(", null).show()
                false
            })
        }
    }

    override fun createStyleOptions(): StyleOptions {
        return StyleOptions().apply {
            selectedListItemBackgroundColor = ThemeHandler.getColor(this@MainActivity, R.attr.inputElementColor, -1)
            listItemTextColor = textColor
            listViewBackgroundColor = backgroundColor
            headerTextColor = textColor
            alphaNormal = 1f
            alphaSelected = 1f
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = false
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (currentFragment is MainFragment) {
                handled = true
                (currentFragment as MainFragment).toggleVPN()
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> if (currentFragment is MainFragment) {
                if ((currentFragment as MainFragment).toggleCurrentInputFocus()) handled = true
            }
        }
        return handled || super.onKeyDown(keyCode, event)
    }

    private fun setDrawableColor(drawable: Drawable): Drawable {
        DrawableCompat.setTint(drawable.mutate(), navDrawableColor)
        return drawable
    }

    fun rateApp() {
        val appPackageName = this.packageName
        LogFactory.writeMessage(this, LOG_TAG, "Opening site to rate app")
        try {
            LogFactory.writeMessage(this, LOG_TAG, "Trying to open market")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            LogFactory.writeMessage(this, LOG_TAG, "Market was opened")
        } catch (e: ActivityNotFoundException) {
            LogFactory.writeMessage(this, LOG_TAG, "Market not present. Opening with general ACTION_VIEW")
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }

        Preferences.getInstance(this).put("rated", true)
    }

    override fun importStarted(combinedLines: Int) {
        val loadingDialog = LoadingDialog(this, ThemeHandler.getDialogTheme(this),
                getString(R.string.importing_x_rules).replace("[x]", combinedLines.toString() + ""),
                getString(R.string.info_importing_rules_app_unusable))
        loadingDialog.setCancelable(false)
        loadingDialog.setCanceledOnTouchOutside(false)
        loadingDialog.show()
        importingRules = true
        updateConfiguration()
        importFinishedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                loadingDialog.dismiss()
                unregisterReceiver(this)
                importFinishedReceiver = null
                importingRules = false
                updateConfiguration()
            }
        }
        registerReceiver(importFinishedReceiver, IntentFilter(RuleImportService.BROADCAST_IMPORT_FINISHED))
    }

    override fun getConfiguration(): Configuration = Configuration.withDefaults()
    override fun getDefaultItem(): DrawerItem = drawerItems[0]
    override fun onItemClicked(item: DrawerItem, handle: Boolean) {}
    override fun useItemBackStack(): Boolean = true
    override fun maxBackStackRecursion(): Int = 5
}