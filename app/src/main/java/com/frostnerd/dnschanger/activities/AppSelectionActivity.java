package com.frostnerd.dnschanger.activities;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.frostnerd.dnschanger.R;

import java.util.ArrayList;
import java.util.Collections;
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
public class AppSelectionActivity extends AppCompatActivity {
    private long lastBackPress;
    private ArrayList<String> currentSelected;
    private RecyclerView appList;
    private RecyclerView.LayoutManager listLayoutManager;
    private AppListAdapter listAdapter;
    private boolean changed;
    private String infoText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);
        appList = (RecyclerView) findViewById(R.id.app_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        currentSelected = getIntent() != null && getIntent().hasExtra("apps") ? getIntent().getStringArrayListExtra("apps") : new ArrayList<String>();
        infoText = getIntent() != null && getIntent().hasExtra("infoText") ? getIntent().getStringExtra("infoText") : null;

        listLayoutManager = new LinearLayoutManager(this);
        appList.setLayoutManager(listLayoutManager);
        appList.setHasFixedSize(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                listAdapter = new AppListAdapter();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        appList.setAdapter(listAdapter);
                    }
                });
            }
        }).start();
        //Preferences.getStringSet(this, "autopause_apps");
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
            setResult(RESULT_OK, new Intent().putExtra("apps", currentSelected));
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
                //if (!entry.isSystemApp()) apps.add(entry);
                apps.add(entry);
            }
            Collections.sort(apps);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder((RelativeLayout) getLayoutInflater().inflate(viewType == 0 ? R.layout.row_appselect_info : R.layout.row_app_entry, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (holder.type == 0){
                ((TextView)holder.contentView.findViewById(R.id.text)).setText(infoText);
            }else{
                int offSet = infoText != null ? 1 : 0;
                ((ImageView) holder.contentView.findViewById(R.id.app_image)).setImageDrawable(apps.get(position - offSet).getIcon());
                ((TextView) holder.contentView.findViewById(R.id.app_title)).setText(apps.get(position - offSet).getTitle());
                if (currentSelected.contains(apps.get(position - offSet).packageName))
                    ((CheckBox) holder.contentView.findViewById(R.id.app_selected_indicator)).setChecked(true);
                holder.contentView.setClickable(true);
                holder.contentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CheckBox) v.findViewById(R.id.app_selected_indicator)).toggle();
                    }
                });
                ((CheckBox) holder.contentView.findViewById(R.id.app_selected_indicator)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked)currentSelected.add(holder.appEntry.packageName);
                        else currentSelected.remove(holder.appEntry.packageName);
                        changed = true;
                    }
                });
                holder.appEntry = apps.get(position - offSet);
            }

        }

        @Override
        public int getItemViewType(int position) {
            return (position == 0 && infoText != null) ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return apps.size() + (infoText != null ? 1 : 0);
        }

        public final class ViewHolder extends RecyclerView.ViewHolder {
            private RelativeLayout contentView;
            private AppEntry appEntry;
            private int type;

            public ViewHolder(RelativeLayout layout, int type) {
                super(layout);
                this.contentView = layout;
                this.type = type;
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
