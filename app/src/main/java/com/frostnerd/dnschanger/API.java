package com.frostnerd.dnschanger;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.frostnerd.dnschanger.services.DNSVpnService;

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
public class API {
    public static final String BROADCAST_SERVICE_STATUS_CHANGE = "com.frostnerd.dnschanger.VPN_SERVICE_CHANGE";
    public static final String BROADCAST_SERVICE_STATE_REQUEST = "com.frostnerd.dnschanger.VPN_STATE_CHANGE";
    public static String TAG = "Debug";

    public static boolean checkVPNServiceRunning(Context c){
        ActivityManager am = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        String name = DNSVpnService.class.getName();
        for(ActivityManager.RunningServiceInfo service: am.getRunningServices(Integer.MAX_VALUE)){
            if(name.equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }

    public static boolean isTaskerInstalled(Context context){
        List<ApplicationInfo> packages;
        packages = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals("net.dinglisch.android.taskerm"))return true;
        }
        return false;
    }

    public static boolean hasUsageStatsPermission(Context context){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)return true;
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
}
