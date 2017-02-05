package com.android.huxq17.rascalkiller.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.andbase.tractor.listener.impl.LoadListenerImpl;
import com.andbase.tractor.task.TaskPool;
import com.android.huxq17.rascalkiller.bean.AppInfo;
import com.android.huxq17.rascalkiller.bean.SystemInfo;
import com.android.huxq17.rascalkiller.task.CleanProcessTask;
import com.jaredrummler.android.processes.ProcessManager;
import com.jaredrummler.android.processes.models.AndroidAppProcess;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by huxq17 on 2016/1/13.
 */
public class Utils {
    public static ArrayList<AppInfo> getApps(Context context) {
        ArrayList<AppInfo> applist = new ArrayList<>();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> list = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (list == null) {
            return applist;
        }
        for (ActivityManager.RunningServiceInfo processInfo : list) {
            AppInfo appInfo = new AppInfo();
            ApplicationInfo packageInfo = null;
            try {
                PackageManager packageManager = context.getPackageManager();
                packageInfo = packageManager.getApplicationInfo(processInfo.process, 0);
                appInfo.appName = packageManager.getApplicationLabel(packageInfo).toString();
                appInfo.appIcon = packageManager.getApplicationIcon(packageInfo);
                appInfo.packageName = packageInfo.packageName;

                if (!isSystemApplication(context, appInfo.packageName) && !applist.contains(appInfo)) {
                    applist.add(appInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return applist;
    }

    public static boolean hasAppRunning(Context context, String packageName) {
        if (SystemInfo.apiVersion < Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> apps = activityManager.getRunningAppProcesses();

            if (apps != null && apps.size() > 0) {
                for (ActivityManager.RunningAppProcessInfo taskInfo : apps) {
                    if (taskInfo != null && context.getPackageName().equals(taskInfo.processName)) {
                        continue;
                    }
                    if (taskInfo != null && taskInfo.processName.equals(packageName)) {
                        return true;
                    }
                }
            }
        } else {
            List<AndroidAppProcess> processInfos = ProcessManager.getRunningAppProcesses();
            PackageManager pm = context.getPackageManager();
            for (AndroidAppProcess processInfo : processInfos) {
                if (context.getPackageName().equals(processInfo.getPackageName())) {
                    continue;
                }
                try {
                    pm.getApplicationInfo(processInfo.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
                if (packageName.equals(processInfo.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ArrayList<AppInfo> getRunningAppsInternal(Context context, List<ActivityManager.RunningAppProcessInfo> apps) {
        ArrayList<AppInfo> applist = new ArrayList<>();
        if (apps != null && apps.size() > 0) {
            PackageManager pm = context.getPackageManager();
            PackageUtil pi = new PackageUtil(context);
            for (ActivityManager.RunningAppProcessInfo taskInfo : apps) {
                if (taskInfo.processName.equals(context.getPackageName())) {
                    continue;
                }
                try {
                    AppInfo appInfo = new AppInfo();
                    appInfo.appName = pi.getApplicationInfo(taskInfo.processName).loadLabel(pm).toString();
                    appInfo.appIcon = pi.getApplicationInfo(taskInfo.processName).loadIcon(pm);
                    appInfo.packageName = taskInfo.processName;
                    if (!isSystemApplication(context, appInfo.packageName) && !applist.contains(appInfo)) {
                        applist.add(appInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return applist;
    }

    private static boolean isTrustedApp(String packagename, Context context) {
        if (packagename.equals("com.netease.mobimail") || packagename.equals(context.getPackageName()) || isInputMethodApp(context, packagename)) {
            return true;
        }
        return false;
    }

    public static ArrayList<AppInfo> getRunningApps(Context context) {
        if (SystemInfo.apiVersion < Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> apps = activityManager.getRunningAppProcesses();
            return getRunningAppsInternal(context, apps);
        } else {
            PackageManager pm = context.getPackageManager();

            List<AndroidAppProcess> processInfos = ProcessManager.getRunningAppProcesses();
            ArrayList<AppInfo> taskinfos = new ArrayList<>();
            for (AndroidAppProcess processInfo : processInfos) {

                String packageName = processInfo.getPackageName();
                if (isTrustedApp(packageName, context)) {
                    continue;
                }
                AppInfo info = new AppInfo();
                info.packageName = packageName;
//                LogUtils.e("test packageName=" + packageName + ";processNum=" + processInfo.name);
                ApplicationInfo applicationInfo;
                try {
                    applicationInfo = pm.getApplicationInfo(packageName, 0);
                    obtainProcess(applicationInfo, pm, taskinfos, info, processInfo);
                } catch (PackageManager.NameNotFoundException e) {
                    String[] packages = pm.getPackagesForUid(processInfo.uid);
                    for (String pkg : packages) {
                        if (isTrustedApp(pkg, context)) {
                            continue;
                        }
                        try {
                            applicationInfo = pm.getApplicationInfo(pkg, 0);
                            info = new AppInfo();
                            info.packageName = pkg;
                            obtainProcess(applicationInfo, pm, taskinfos, info, processInfo);
                        } catch (PackageManager.NameNotFoundException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
            return taskinfos;
        }
    }

    public synchronized static boolean isInputMethodApp(Context context, String strPkgName) {
        PackageManager pkm = context.getPackageManager();
        boolean bIsIME = false;
        PackageInfo pkgInfo;
        try {
            pkgInfo = pkm.getPackageInfo(strPkgName, PackageManager.GET_SERVICES);
            ServiceInfo[] servicesInfos = pkgInfo.services;
            if (null != servicesInfos) {
                for (int i = 0; i < servicesInfos.length; i++) {
                    ServiceInfo sInfo = servicesInfos[i];
                    if (null != sInfo.permission && sInfo.permission.equals("android.permission.BIND_INPUT_METHOD")) {
                        bIsIME = true;
                        break;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bIsIME;
    }

    private static void obtainProcess(ApplicationInfo applicationInfo, PackageManager pm, ArrayList<AppInfo> taskinfos, AppInfo info, AndroidAppProcess processInfo) {
        if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            // 用户进程
            info.appIcon = applicationInfo.loadIcon(pm);
            info.appName = applicationInfo.loadLabel(pm).toString();
            int index = taskinfos.indexOf(info);
            if (index == -1) {
                taskinfos.add(info);
            } else {
                info = taskinfos.get(index);
            }
            info.addProcess(processInfo.name, processInfo.pid);
        } else {
            // 系统进程
        }
    }

//    public static String getForegroundApp(Context context) {
//        List<UsageStats> queryUsageStats = getUsageStats(context);
//        UsageStats recentStats = null;
//        if (queryUsageStats != null) {
//            for (UsageStats usageStats : queryUsageStats) {
//                if (recentStats == null || recentStats.getLastTimeUsed() < usageStats.getLastTimeUsed()) {
//                    recentStats = usageStats;
//                }
//            }
//            return recentStats.getPackageName();
//        }
//        return null;
//    }

//    private static List<UsageStats> getUsageStats(Context context) {
//        long ts = System.currentTimeMillis();
//        long start = ts - SystemClock.elapsedRealtime();
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.YEAR, -1);
//        long startTime = calendar.getTimeInMillis();
//
//        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
//        List<UsageStats> queryUsageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, startTime, ts);
//        LogUtils.e("startTime=" + startTime + ";ts=" + ts);
//        if (queryUsageStats == null || queryUsageStats.isEmpty()) {
//            return null;
//        }
//        return queryUsageStats;
//    }

//    public static long getRunningTime(String packageName, Context context) {
//        long time = 0l;
//        List<UsageStats> queryUsageStats = getUsageStats(context);
//        if (queryUsageStats != null) {
//            for (UsageStats usageStats : queryUsageStats) {
//                if (usageStats.getPackageName().equals(packageName)) {
//                    time = usageStats.getTotalTimeInForeground();
////                    LogUtils.e("end=" + usageStats.getLastTimeStamp() + ";start=" + usageStats.getFirstTimeStamp());
//                    break;
//                }
//            }
//        }
//        return time;
//    }

//    public static boolean hasAccessUsageStatsPermission(Context context) {
//        if (getUsageStats(context) == null) {
//            return false;
//        }
//        return true;
//    }

    public static String getTopAppInfoPackageName(Context context) {
        if (SystemInfo.apiVersion < 21) { // 如果版本低于22
            // 获取到activity的管理的类
            ActivityManager m = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            // 获取最近的一个运行的任务的信息
            List<ActivityManager.RunningTaskInfo> tasks = m.getRunningTasks(1);
            if (tasks != null && tasks.size() > 0) { // 如果集合不是空的
                // 返回任务栈中最上面的一个
                ActivityManager.RunningTaskInfo info = m.getRunningTasks(1).get(0);
                // 获取到应用的包名
//                 String packageName =
//                 info.topActivity.getPackageName();
                return info.baseActivity.getPackageName();
            } else {
                return "";
            }
        } else {
            final int PROCESS_STATE_TOP = 2;
            try {
                // 获取正在运行的进程应用的信息实体中的一个字段,通过反射获取出来
                Field processStateField = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
                // 获取所有的正在运行的进程应用信息实体对象
                List<ActivityManager.RunningAppProcessInfo> processes = ((ActivityManager) context
                        .getSystemService(ACTIVITY_SERVICE)).getRunningAppProcesses();
                // 循环所有的进程,检测某一个进程的状态是最上面,也是就最近运行的一个应用的状态的时候,就返回这个应用的包名
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                            && process.importanceReasonCode == 0) {
                        int state = processStateField.getInt(process);
                        if (state == PROCESS_STATE_TOP) { // 如果这个实体对象的状态为最近的运行应用
                            String[] packname = process.pkgList;
                            // 返回应用的包名
                            return packname[0];
                        }
                    }
                }
            } catch (Exception e) {
            }
            return "";
        }
    }

    /**
     * 跳转到某一个包名的应用详情界面
     */
    @SuppressLint("InlinedApi")
    public static void showInstalledAppDetails(Context context, String packageName) throws Exception {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    public static boolean isSystemApplication(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo packageInfo = manager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
//            // 1
//            if (new File("/data/app/" + packageInfo.packageName + ".apk").exists()) {
//                return true;
//            }
//            // 2
//            if (packageInfo.versionName != null && packageInfo.applicationInfo.uid > 10000) {
//                return true;
//            }
//            // 3
            if ((packageInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 结束进程,执行操作调用即可
     */
    public static void killProcess(Context context, String packageName, LoadListenerImpl listener) {
        if (listener != null) {
            listener.setDismissTime(0);
        }
        Process process;
        OutputStream out = null;
        String cmd = "am force-stop " + packageName + " \n";
        try {
            process = Runtime.getRuntime().exec("su");
            out = process.getOutputStream();
            out.write(cmd.getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(out);
        }
        TaskPool.getInstance().execute(new CleanProcessTask(context, packageName, listener, null));
    }

    public static void requestSu() {
        try {
            Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
