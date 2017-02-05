package com.android.huxq17.rascalkiller.task;

import android.content.Context;
import android.os.SystemClock;

import com.andbase.tractor.listener.LoadListener;
import com.andbase.tractor.task.Task;
import com.android.huxq17.rascalkiller.utils.Utils;

/**
 * Created by 2144 on 2017/1/13.
 */

public class CleanProcessTask extends Task {
    private String packageName;
    private Context context;
    private static final long TIMEOUT = 10000;

    public CleanProcessTask(Context context, String packageName, LoadListener listener, Object tag) {
        super(tag, listener);
        this.packageName = packageName;
        this.context = context;
    }

    @Override
    public void onRun() {
        boolean hasAppRunning = Utils.hasAppRunning(context, packageName);
        long startTime = System.currentTimeMillis();
        while (hasAppRunning && (System.currentTimeMillis() - startTime <= TIMEOUT)) {
            hasAppRunning = Utils.hasAppRunning(context, packageName);
            SystemClock.sleep(30);
        }
    }

    @Override
    public void cancelTask() {

    }
}
