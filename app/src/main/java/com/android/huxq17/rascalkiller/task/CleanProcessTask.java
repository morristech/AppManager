package com.android.huxq17.rascalkiller.task;

import android.content.Context;
import android.os.SystemClock;

import com.andbase.tractor.listener.LoadListener;
import com.andbase.tractor.task.Task;
import com.android.huxq17.rascalkiller.bean.AppInfo;
import com.android.huxq17.rascalkiller.utils.Utils;

import java.util.ArrayList;

/**
 * Created by 2144 on 2017/1/13.
 */

public class CleanProcessTask extends Task {
    private String[] packages;
    private Context context;
    private static final long TIMEOUT = 10000;

    public CleanProcessTask(Context context, String[] packageName, LoadListener listener, Object tag) {
        super(tag, listener);
        this.packages = packageName;
        this.context = context;
    }

    @Override
    public void onRun() {
        boolean hasAppRunning;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime <= TIMEOUT) {
            ArrayList<AppInfo> apps = Utils.getRunningApps(context);
            hasAppRunning = false;
            for (String pkg : packages) {
                AppInfo appinfo = new AppInfo();
                appinfo.packageName = pkg;
                if (apps.contains(appinfo)) {
                    hasAppRunning = true;
                }
            }
            if (!hasAppRunning) {
                break;
            }
            SystemClock.sleep(30);
        }
    }

    @Override
    public void cancelTask() {

    }
}
