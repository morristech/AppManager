package com.android.huxq17.rascalkiller.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.huxq17.rascalkiller.MainActivity;
import com.android.huxq17.rascalkiller.R;
import com.android.huxq17.rascalkiller.bean.AppInfo;
import com.andview.refreshview.recyclerview.BaseRecyclerAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends BaseRecyclerAdapter<AppListAdapter.SimpleAdapterViewHolder> {
    private List<AppInfo> list;
    private boolean isSelectMode = false;
    private MainActivity parent;
    private List<String> selectedApp = new ArrayList<>();
    private OnClickListener clickListener;
    private SelectedItem lastSelected;

    public AppListAdapter(List<AppInfo> list, MainActivity activity) {
        this.list = list;
        this.parent = activity;
    }

    public void onBindViewHolder(final SimpleAdapterViewHolder holder,
                                 int position, boolean isItem) {
        final AppInfo appInfo = list.get(position);
        holder.appName.setText(appInfo.appName);
        holder.appIcon.setImageDrawable(appInfo.appIcon);
        holder.appProcessNum.setText("进程数:" + appInfo.getProcessNum());
        holder.itemView.setTag(appInfo.packageName);
        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String packageName = (String) v.getTag();
                    longClickListener.onItemLongClick(v, packageName);
                    if (!isSelectMode) {
                        v.setSelected(true);
                        parent.startSupportActionMode();
                        isSelectMode = true;
                        selectApp(packageName);
                        return true;
                    }
                    return !isSelectMode ? true : false;
                }
            });
        }
        boolean isItemSelected = selectedApp.contains(appInfo.packageName);
        holder.itemView.setSelected(isItemSelected);
        if (isItemSelected && !isSelectMode && lastSelected != null) {
            lastSelected.update(holder.itemView, appInfo.packageName);
        }
        if (clickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String packageName = (String) v.getTag();
                    if (v.isSelected()) {
                        unSelectApp(packageName);
                    } else {
                        if (!isSelectMode) {
                            if (lastSelected != null) {
                                unSelectApp(lastSelected.packageName);
                                View view = lastSelected.getView();
                                if (view != null) {
                                    view.setSelected(false);
                                }
                                lastSelected.update(v, packageName);
                            } else {
                                lastSelected = new SelectedItem(v, packageName);
                            }
                        }
                        selectApp(packageName);
                    }
                    v.setSelected(!v.isSelected());
                    clickListener.onClick(v, appInfo.packageName);
                }
            });
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View v, String packageName);
    }

    public interface OnClickListener {
        void onClick(View v, String packageName);
    }

    public List<String> getSelectedApp() {
        List<String> selected = new ArrayList<>();
        selected.addAll(selectedApp);
        return selected;
    }

    public void existActionMode() {
        isSelectMode = false;
        selectedApp.clear();
        notifyDataSetChanged();
        parent.showMenu(false);
    }

    public void selectApp(String packageName) {
        if (!selectedApp.contains(packageName))
            selectedApp.add(packageName);
    }

    public void unSelectApp(String packageName) {
        selectedApp.remove(packageName);
    }

    public void unSelectAllapp() {
        selectedApp.clear();
    }

    private OnItemLongClickListener longClickListener;

    public void setOnLongClickListener(OnItemLongClickListener listener) {
        longClickListener = listener;
    }

    public void setOnClickListener(OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setData(ArrayList<AppInfo> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public int getAdapterItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    @Override
    public SimpleAdapterViewHolder getViewHolder(View view) {
        return new SimpleAdapterViewHolder(view, false);
    }

    @Override
    public SimpleAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType, boolean isItem) {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_applist, parent, false);

        SimpleAdapterViewHolder vh = new SimpleAdapterViewHolder(v, true);
        return vh;
    }

    public class SimpleAdapterViewHolder extends RecyclerView.ViewHolder {

        public TextView appName;
        public ImageView appIcon;
        public TextView appProcessNum;

        public SimpleAdapterViewHolder(View itemView, boolean isItem) {
            super(itemView);
            if (isItem) {
                appName = (TextView) itemView.findViewById(R.id.tv_appname);
                appIcon = (ImageView) itemView.findViewById(R.id.iv_appicon);
                appProcessNum = (TextView) itemView.findViewById(R.id.tv_process_num);
            }
        }
    }

    public void insert(AppInfo person, int position) {
        insert(list, person, position);
    }

    public void remove(int position) {
        remove(list, position);
    }

    public void clear() {
        clear(list);
    }

    public AppInfo getItem(int position) {
        if (position < list.size())
            return list.get(position);
        else
            return null;
    }

    class SelectedItem {

        public WeakReference<View> selectedView;
        public String packageName;

        public SelectedItem(View view, String packageName) {
            selectedView = new WeakReference<>(view);
            this.packageName = packageName;
        }

        public void update(View view, String packageName) {
            selectedView = new WeakReference<>(view);
            this.packageName = packageName;
        }

        public View getView() {
            if (selectedView != null) {
                return selectedView.get();
            }
            return null;
        }

        @Override
        public String toString() {
            return "SelectedItem{" +
                    "packageName='" + packageName + '\'' +
                    '}';
        }
    }
}