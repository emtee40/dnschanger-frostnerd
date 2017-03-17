package com.frostnerd.dnschanger;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.frostnerd.utils.general.SortUtils;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class AutoPauseAppSelectActivity extends AppCompatActivity {
    private long lastBackPress;
    private Set<String> currentSelected;
    private RecyclerView appList;
    private RecyclerView.LayoutManager listLayoutManager;
    private AppListAdapter listAdapter;
    private boolean changed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autopause_app_select);
        appList = (RecyclerView) findViewById(R.id.app_list);

        listLayoutManager = new LinearLayoutManager(this);
        appList.setLayoutManager(listLayoutManager);
        appList.setHasFixedSize(true);
        appList.setAdapter(listAdapter = new AppListAdapter());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        currentSelected = Preferences.getStringSet(this, "autopause_apps");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_autopause_appselect, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastBackPress <= 1500 || !changed) {
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        } else {
            lastBackPress = System.currentTimeMillis();
            Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done || item.getItemId() == android.R.id.home) {
            Preferences.put(this, "autopause_apps", currentSelected);
            Preferences.put(this, "autopause_apps_count", currentSelected.size());
            setResult(RESULT_OK, new Intent().putExtra("count", currentSelected.size()));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private final class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
        private List<AppEntry> apps = new ArrayList<>();

        public AppListAdapter() {
            List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            AppEntry entry;
            for (ApplicationInfo packageInfo : packages) {
                entry = new AppEntry(getPackageManager(), packageInfo);
                if (!entry.isSystemApp()) apps.add(entry);
            }
            SortUtils.quickSort(apps);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder((RelativeLayout) getLayoutInflater().inflate(viewType == 0 ? R.layout.row_autopause_info : R.layout.row_app_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (position == 0) return;
            ((ImageView) holder.contentView.findViewById(R.id.app_image)).setImageDrawable(apps.get(position - 1).getIcon());
            ((TextView) holder.contentView.findViewById(R.id.app_title)).setText(apps.get(position - 1).getTitle());
            if (currentSelected.contains(apps.get(position - 1).packageName))
                ((CheckBox) holder.contentView.findViewById(R.id.app_selected_indicator)).setChecked(true);
            ((CheckBox) holder.contentView.findViewById(R.id.app_selected_indicator)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked)currentSelected.add(holder.appEntry.packageName);
                    else currentSelected.remove(holder.appEntry.packageName);
                    changed = true;
                }
            });
            holder.appEntry = apps.get(position - 1);
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return apps.size() + 1;
        }

        public final class ViewHolder extends RecyclerView.ViewHolder {
            private RelativeLayout contentView;
            private AppEntry appEntry;

            public ViewHolder(RelativeLayout layout) {
                super(layout);
                this.contentView = layout;
            }
        }
    }

    private class AppEntry implements Comparable<AppEntry> {
        private ApplicationInfo info;
        private Drawable icon;
        private String title;
        private String packageName;

        public AppEntry(PackageManager pm, ApplicationInfo info) {
            this.info = info;
            icon = info.loadIcon(pm);
            title = pm.getApplicationLabel(info).toString();
            packageName = info.packageName;
        }

        public ApplicationInfo getRawInfo() {
            return info;
        }

        public String getTitle() {
            return title;
        }

        public boolean isSystemApp() {
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        private Drawable getIcon() {
            return icon;
        }

        @Override
        public int compareTo(@NonNull AppEntry o) {
            return title.compareTo(o.title);
        }
    }
}
