package com.android.huxq17.rascalkiller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.andbase.tractor.listener.LoadListener;
import com.andbase.tractor.listener.impl.LoadListenerImpl;
import com.andbase.tractor.task.Task;
import com.andbase.tractor.task.TaskPool;
import com.android.huxq17.rascalkiller.adapter.AppListAdapter;
import com.android.huxq17.rascalkiller.base.BaseActivity;
import com.android.huxq17.rascalkiller.bean.AppInfo;
import com.android.huxq17.rascalkiller.utils.Utils;
import com.andview.refreshview.XRefreshView;
import com.andview.refreshview.utils.LogUtils;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements ActionMode.Callback {
    private RecyclerView recyclerView;
    private XRefreshView xRefreshView;
    private AppListAdapter adapter;
    private LinearLayoutManager layoutmanager;
    private ArrayList<AppInfo> appList = new ArrayList<>();
    private boolean mRunning;
    private ScreenStatusReceiver mScreenStatusReceiver;
    private Toolbar mToolbar;
    private FloatingActionButton floatingActionButton;
    private FloatingActionMenu floatingActionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRunning = true;
        initView();
        configView();
        registSreenStatusReceiver();
        Utils.requestSu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRunning = false;
        unregisterReceiver(mScreenStatusReceiver);
    }

    private void registSreenStatusReceiver() {
        mScreenStatusReceiver = new ScreenStatusReceiver();
        IntentFilter screenStatusIF = new IntentFilter();
        screenStatusIF.addAction(Intent.ACTION_SCREEN_ON);
        screenStatusIF.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStatusReceiver, screenStatusIF);
    }

    LoadListener listener = new LoadListenerImpl() {

        @Override
        public void onSuccess(Object result) {
            super.onSuccess(result);
            xRefreshView.stopRefresh();
            adapter.setData(appList);
        }
    };

    private void loadData() {
        TaskPool.getInstance().execute(new Task(this, listener) {
            @Override
            public void onRun() {
                appList = Utils.getRunningApps(getApplicationContext());
            }

            @Override
            public void cancelTask() {
            }
        });
    }

    private void initView() {
        recyclerView = (RecyclerView) f(R.id.rv_applist);
        xRefreshView = (XRefreshView) f(R.id.xrefreshview);
        mToolbar = (Toolbar) f(R.id.toolbar);
        setSupportActionBar(mToolbar);
        layoutmanager = new LinearLayoutManager(this);
//        floatingActionButton = (FloatingActionButton) f(R.id.fab);
    }

    private void configView() {
        LogUtils.enableLog(false);
        recyclerView.setHasFixedSize(true);
        xRefreshView.setPinnedTime(1000);
        adapter = new AppListAdapter(appList, this);
        recyclerView.setLayoutManager(layoutmanager);
        xRefreshView.setMoveFootWhenDisablePullLoadMore(false);
        adapter.setOnLongClickListener(new AppListAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View v, String packageName) {

            }
        });
        adapter.setOnClickListener(new AppListAdapter.OnClickListener() {
            @Override
            public void onClick(View v, String packageName) {
                if (adapter.getSelectedApp().size() > 0) {
                    showMenu(true);
                } else {
                    showMenu(false);
                }
            }
        });
        recyclerView.setAdapter(adapter);
        xRefreshView.setXRefreshViewListener(new XRefreshView.SimpleXRefreshListener() {

            @Override
            public void onRefresh() {
                loadData();
            }
        });
//        configFloatingButton();
    }

    private void stopApp(String packageName) {
        Utils.killProcess(MainActivity.this, packageName, new LoadListenerImpl(MainActivity.this, "清理中...") {
            @Override
            public void onSuccess(Object result) {
                super.onSuccess(result);
                loadData();
            }
        });
    }

    public void startSupportActionMode() {
        this.startSupportActionMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void showMenu(boolean show) {
        mToolbar.getMenu().findItem(R.id.action_clear).setVisible(show);
        mToolbar.getMenu().findItem(R.id.action_start).setVisible(show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear) {
            stopApp();
            showMenu(false);
            return true;
        } else if (item.getItemId() == R.id.action_start) {
            List<String> packages = adapter.getSelectedApp();
            if (packages.size() == 0) return super.onOptionsItemSelected(item);
            String packageName = adapter.getSelectedApp().get(0);
            if (!TextUtils.isEmpty(packageName)) {
                Utils.startApp(this, packageName);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        showMenu(false);
        return super.onPrepareOptionsMenu(menu);
    }

    private void stopApp() {
        List<String> selectedApp = adapter.getSelectedApp();
        if (selectedApp.size() > 0) {
            for (String packageName : selectedApp) {
                adapter.unSelectApp(packageName);
                stopApp(packageName);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.main_action_menu, menu);
        return true;
    }

    @Override
    public void finish() {
        if (adapter.getSelectedApp().size() == 0) {
            super.finish();
        } else {
            adapter.existActionMode();
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_clear) {
            stopApp();
            mode.finish();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        adapter.existActionMode();
    }

    class ScreenStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Utils.killProcess(getApplicationContext(), "com.tencent.mobileqq", null);
                Utils.killProcess(getApplicationContext(), "com.eg.android.AlipayGphone", null);
            }
        }
    }

//    private boolean isTouchFloatingButton(MotionEvent ev) {
//        if (floatingActionButton != null && floatingActionButton.getVisibility() == VISIBLE) {
//            Rect bounds = new Rect();
//            floatingActionButton.getGlobalVisibleRect(bounds);
//            int x = (int) ev.getRawX();
//            int y = (int) ev.getRawY();
//            if (bounds.contains(x, y)) {
//                return true;
//            } else {
//                return false;
//            }
//        }
//        return false;
//    }

//    private void configFloatingButton() {
//        SubActionButton.Builder rLSubBuilder = new SubActionButton.Builder(this);
//        ImageView rlIcon1 = new ImageView(this);
//        ImageView rlIcon2 = new ImageView(this);
//        ImageView rlIcon3 = new ImageView(this);
//        ImageView rlIcon4 = new ImageView(this);
//
//        rlIcon1.setImageDrawable(getResources().getDrawable(R.mipmap.ic_action_chat_light));
//        rlIcon2.setImageDrawable(getResources().getDrawable(R.mipmap.ic_action_camera_light));
//        rlIcon3.setImageDrawable(getResources().getDrawable(R.mipmap.ic_action_video_light));
//        rlIcon4.setImageDrawable(getResources().getDrawable(R.mipmap.ic_action_place_light));
//
//        // Build the main_menu with default options: light theme, 90 degrees, 72dp radius.
//        // Set 4 default SubActionButtons
//        floatingActionMenu = new FloatingActionMenu.Builder(this)
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon1).build())
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon2).build())
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon3).build())
//                .addSubActionView(rLSubBuilder.setContentView(rlIcon4).build())
//                .attachTo(floatingActionButton)
//                .build();
//        floatingActionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
//            @Override
//            public void onMenuOpened(FloatingActionMenu menu) {
//                // Rotate the icon of rightLowerButton 45 degrees clockwise
//                floatingActionButton.setRotation(0);
//                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 45);
//                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(floatingActionButton, pvhR);
//                animation.start();
//            }
//
//            @Override
//            public void onMenuClosed(FloatingActionMenu menu) {
//                floatingActionButton.setRotation(45);
//                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 0);
//                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(floatingActionButton, pvhR);
//                animation.start();
//            }
//        });
//    }
//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        int action = ev.getAction();
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                if (floatingActionMenu.isOpen()&&!isTouchFloatingButton(ev)) {
//                    floatingActionMenu.close(false);
//                }
//                break;
//        }
//        return super.dispatchTouchEvent(ev);
//    }

}
