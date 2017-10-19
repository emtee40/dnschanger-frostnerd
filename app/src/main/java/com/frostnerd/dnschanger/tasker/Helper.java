package com.frostnerd.dnschanger.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.frostnerd.dnschanger.database.entities.IPPortPair;

import java.util.ArrayList;

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
    public static final String BUNDLE_EXTRA_DNS1 = "com.frostnerd.dnschanger.dns1",
            BUNDLE_EXTRA_DNS2 = "com.frostnerd.dnschanger.dns2",
            BUNDLE_EXTRA_DNS1V6 = "com.frostnerd.dnschanger.dns1v6",
            BUNDLE_EXTRA_DNS2V6 = "com.frostnerd.dnschanger.dns2v6",
            BUNDLE_EXTRA_STOP_DNS = "com.frostnerd.dnschanger.stopdns",
            BUNDLE_EXTRA_PAUSE_DNS = "com.frostnerd.dnschanger.pausedns",
            BUNDLE_EXTRA_RESUME_DNS = "com.frostnerd.dnschanger.resumedns",
            BUNDLE_EXTRA_SERVERS = "com.frostnerd.dnschanger.servers";

    public static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    public static final String ACTION_FIRE_SETTINGS = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    public static final String ACTION_EDIT_SETTINGS = "com.twofortyfouram.locale.intent.action.EDIT_SETTING";
    public static final String EXTRA_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";

    public static boolean scrub(final Intent intent) {
        if (null == intent) return false;
        return scrub(intent.getExtras());
    }

    public static boolean scrub(final Bundle bundle) {
        if (null == bundle) return false;
        try {
            bundle.containsKey(null);
        } catch (final Exception e) {
            bundle.clear();
            return true;
        }
        return false;
    }

    public static boolean isBundleValid(final Bundle bundle) {
        if (null == bundle) return false;
        if (bundle.containsKey(BUNDLE_EXTRA_STOP_DNS) || bundle.containsKey(BUNDLE_EXTRA_RESUME_DNS) || bundle.containsKey(BUNDLE_EXTRA_PAUSE_DNS))
            return true;
        if (bundle.containsKey(BUNDLE_EXTRA_SERVERS)) return true;
        if (!bundle.containsKey(BUNDLE_EXTRA_DNS1) || !bundle.containsKey(BUNDLE_EXTRA_DNS2) ||
                !bundle.containsKey(BUNDLE_EXTRA_DNS1V6) || !bundle.containsKey(BUNDLE_EXTRA_DNS2V6)) {
            return false;
        }
        String dns1 = bundle.getString(BUNDLE_EXTRA_DNS1), dns1v6 = bundle.getString(BUNDLE_EXTRA_DNS1V6);

        return !(bundle.keySet().size() != 4 || TextUtils.isEmpty(dns1) || TextUtils.isEmpty(dns1v6));
    }

    public static Bundle createBundle(final IPPortPair dns1, final IPPortPair dns2, final IPPortPair dns1v6, final IPPortPair dns2v6) {
        final Bundle bundle = new Bundle();
        bundle.putSerializable("servers", new ArrayList<IPPortPair>() {
            {
                add(dns1);
                add(dns2);
                add(dns1v6);
                add(dns2v6);
            }

            @Override
            public boolean add(IPPortPair pair) {
                return !TextUtils.isEmpty(pair.getAddress()) && super.add(pair);
            }
        });
        return bundle;
    }
}
