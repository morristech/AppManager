package com.android.huxq17.rascalkiller.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.List;

public class PackageUtil {

    // ApplicationInfo 类，保存了普通应用程序的信息，主要是指Manifest.xml中application标签中的信息

    private List<ApplicationInfo> allAppList;


    public PackageUtil(Context context) {
        // 通过包管理器，检索所有的应用程序（包括卸载）与数据目录
        PackageManager pm = context.getApplicationContext().getPackageManager();
        allAppList = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        pm.getInstalledPackages(0);
    }


    public ApplicationInfo getApplicationInfo(String appName) {
        if (appName == null) {
            return null;
        }
        for (ApplicationInfo appinfo : allAppList) {
            if (appName.equals(appinfo.processName)) {
                return appinfo;
            }
        }
        return null;
    }

}
