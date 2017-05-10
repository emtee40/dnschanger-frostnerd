package com.frostnerd.dnschanger.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SimpleItemAnimator;
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
import java.util.TreeSet;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public class AppSelectionActivity extends AppCompatActivity implements SearchView.OnQueryTextListener{
    private long lastBackPress;
    private ArrayList<String> currentSelected;
    private RecyclerView appList;
    private RecyclerView.LayoutManager listLayoutManager;
    private AppListAdapter listAdapter;
    private boolean changed;
    private String infoTextWhitelist, infoTextBlacklist;
    private boolean whiteList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);
        appList = (RecyclerView) findViewById(R.id.app_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        currentSelected = getIntent() != null && getIntent().hasExtra("apps") ? getIntent().getStringArrayListExtra("apps") : new ArrayList<String>();
        infoTextWhitelist = getIntent() != null && getIntent().hasExtra("infoTextWhitelist") ? getIntent().getStringExtra("infoTextWhitelist") : null;
        infoTextBlacklist = getIntent() != null && getIntent().hasExtra("infoTextBlacklist") ? getIntent().getStringExtra("infoTextBlacklist") : null;
        whiteList = getIntent() != null && getIntent().getBooleanExtra("whitelist", false);
        listLayoutManager = new LinearLayoutManager(this);
        appList.setLayoutManager(listLayoutManager);
        appList.setHasFixedSize(true);
        ((SimpleItemAnimator) appList.getItemAnimator()).setSupportsChangeAnimations(false);
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
        ((SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search))).setOnQueryTextListener(this);
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
            setResult(RESULT_OK, new Intent().putExtra("apps", currentSelected).putExtra("whitelist", whiteList));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        listAdapter.filter(newText);
        return true;
    }

    private final class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
        private TreeSet<AppEntry> apps = new TreeSet<>();
        private List<AppEntry> searchedApps = new ArrayList<>();
        private boolean apply = true;

        public AppListAdapter() {
            List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            AppEntry entry;
            for (ApplicationInfo packageInfo : packages) {
                entry = new AppEntry(getPackageManager(), packageInfo);
                //if (!entry.isSystemApp()) apps.add(entry);
                apps.add(entry);
            }
            filter("");
        }

        public void filter(String search){
            searchedApps.clear();
            if(search.equals("")){
                for(AppEntry entry: apps)searchedApps.add(entry);
            }else{
                for(AppEntry entry: apps){
                    if(entry.getTitle().toLowerCase().contains(search.toLowerCase()))searchedApps.add(entry);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder((RelativeLayout) getLayoutInflater().inflate(viewType == 0 ? R.layout.row_appselect_info : R.layout.row_app_entry, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (holder.type == 0){
                CheckBox checkboxWhitelist = ((CheckBox)holder.contentView.findViewById(R.id.checkbox_whitelist)),
                        deselect = ((CheckBox)holder.contentView.findViewById(R.id.deselect_all)),
                        select = ((CheckBox)holder.contentView.findViewById(R.id.select_all));
                checkboxWhitelist.setOnCheckedChangeListener(null);
                deselect.setOnCheckedChangeListener(null);
                select.setOnCheckedChangeListener(null);
                ((TextView)holder.contentView.findViewById(R.id.text)).setText(whiteList ? infoTextWhitelist : infoTextBlacklist);
                checkboxWhitelist.setChecked(whiteList);
                deselect.setChecked(currentSelected.size() == 0);
                select.setChecked(currentSelected.size() == apps.size());
                checkboxWhitelist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        whiteList = isChecked;
                        listAdapter.notifyItemChanged(0);
                    }
                });
                deselect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        currentSelected.clear();
                        notifyItemRangeChanged(0, getItemCount());
                    }
                });
                select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        for(AppEntry entry: apps)currentSelected.add(entry.packageName);
                        notifyItemRangeChanged(0, getItemCount());
                    }
                });
            }else{
                int offSet = 1;
                AppEntry entry = searchedApps.get(position - offSet);
                CheckBox checkBox = (CheckBox) holder.contentView.findViewById(R.id.app_selected_indicator);
                ((ImageView) holder.contentView.findViewById(R.id.app_image)).setImageDrawable(entry.getIcon());
                ((TextView) holder.contentView.findViewById(R.id.app_title)).setText(entry.getTitle());
                holder.contentView.setClickable(true);
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(currentSelected.contains(entry.packageName));
                holder.contentView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CheckBox) v.findViewById(R.id.app_selected_indicator)).toggle();
                    }
                });
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(!apply)return;
                        if (isChecked)currentSelected.add(holder.appEntry.packageName);
                        else currentSelected.remove(holder.appEntry.packageName);
                        listAdapter.notifyItemChanged(0);
                        changed = true;
                    }
                });
                holder.appEntry = entry;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return searchedApps.size() + 1;
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
