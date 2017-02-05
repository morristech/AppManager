package com.android.huxq17.rascalkiller.bean;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huxq17 on 2016/1/13.
 */
public class AppInfo {
    public String appName;
    public String packageName;
    public Drawable appIcon;
    public List<ProcessInfo> processInfos = new ArrayList<>();

    public static class ProcessInfo {
        public String name;
        public int pid;

        public ProcessInfo(int pid, String name) {
            this.name = name;
            this.pid = pid;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof ProcessInfo) {
                ProcessInfo info = (ProcessInfo) o;
                return info.pid == this.pid;
            }
            return super.equals(o);
        }
    }

    public void addProcess(String processName, int pid) {
        ProcessInfo processInfo = new ProcessInfo(pid, processName);
        processInfos.add(processInfo);
    }

    public int getProcessNum() {
        return processInfos.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof AppInfo) {
            AppInfo info = (AppInfo) o;
            return info.packageName.equals(this.packageName);
        }
        return super.equals(o);
    }
}
