package com.frostnerd.dnschanger.tasker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class Helper {
    static final String BUNDLE_EXTRA_DNS1 = "com.frostnerd.dnschanger.dns1",
            BUNDLE_EXTRA_DNS2 = "com.frostnerd.dnschanger.dns2",
            BUNDLE_EXTRA_DNS1V6 = "com.frostnerd.dnschanger.dns1v6",
            BUNDLE_EXTRA_DNS2V6 = "com.frostnerd.dnschanger.dns2v6",
            BUNDLE_EXTRA_STOP_DNS = "com.frostnerd.dnschanger.stopdns",
            BUNDLE_EXTRA_PAUSE_DNS = "com.frostnerd.dnschanger.pausedns",
            BUNDLE_EXTRA_RESUME_DNS = "com.frostnerd.dnschanger.resumedns",
            BUNDLE_EXTRA_V2= "com.frostnerd.dnschanger.v2";

    static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    static final String ACTION_FIRE_SETTINGS = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    public static final String ACTION_EDIT_SETTINGS = "com.twofortyfouram.locale.intent.action.EDIT_SETTING";
    static final String EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";

    static boolean scrub(final Intent intent) {
        return null != intent && scrub(intent.getExtras());
    }

    static boolean scrub(final Bundle bundle) {
        if (null == bundle) return false;
        try {
            bundle.containsKey(null);
        } catch (final Exception e) {
            bundle.clear();
            return true;
        }
        return false;
    }

    static boolean isBundleValid(Context context, Bundle bundle) {
        if (null == bundle) return false;
        if (bundle.containsKey(BUNDLE_EXTRA_STOP_DNS) || bundle.containsKey(BUNDLE_EXTRA_RESUME_DNS) || bundle.containsKey(BUNDLE_EXTRA_PAUSE_DNS))
            return true;
        if (!bundle.containsKey(BUNDLE_EXTRA_DNS1) && !bundle.containsKey(BUNDLE_EXTRA_DNS2) &&
                !bundle.containsKey(BUNDLE_EXTRA_DNS1V6) && !bundle.containsKey(BUNDLE_EXTRA_DNS2V6)) {
            return false;
        }
        String dns1 = bundle.getString(BUNDLE_EXTRA_DNS1), dns1v6 = bundle.getString(BUNDLE_EXTRA_DNS1V6);
        return (PreferencesAccessor.isIPv4Enabled(context) && !TextUtils.isEmpty(dns1)) ||
                (PreferencesAccessor.isIPv6Enabled(context) && !TextUtils.isEmpty(dns1v6));
    }

    static Bundle createBundle(final IPPortPair dns1, final IPPortPair dns2, final IPPortPair dns1v6, final IPPortPair dns2v6) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(BUNDLE_EXTRA_V2, true);
        if(!dns1.isEmpty())bundle.putString(BUNDLE_EXTRA_DNS1, dns1.toString());
        if(!dns2.isEmpty())bundle.putString(BUNDLE_EXTRA_DNS2, dns2.toString());
        if(!dns1v6.isEmpty())bundle.putString(BUNDLE_EXTRA_DNS1V6, dns1v6.toString());
        if(!dns2v6.isEmpty())bundle.putString(BUNDLE_EXTRA_DNS2V6, dns2v6.toString());
        return bundle;
    }
}
