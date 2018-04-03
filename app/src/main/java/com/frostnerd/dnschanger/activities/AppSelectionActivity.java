package com.frostnerd.dnschanger.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.lifecyclehelper.UtilityActivity;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
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
public class AppSelectionActivity extends UtilityActivity implements SearchView.OnQueryTextListener{
    private long lastBackPress;
    private ArrayList<String> currentSelected;
    private RecyclerView appList;
    private AppListAdapter listAdapter;
    private boolean changed;
    private String infoTextWhitelist, infoTextBlacklist;
    private boolean whiteList, onlyInternet, showSystemApps = true;
    private FloatingActionButton fabSettings;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHandler.getAppTheme(this));
        setContentView(R.layout.activity_app_select);
        appList = findViewById(R.id.app_list);
        fabSettings = findViewById(R.id.fab_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        onlyInternet = getIntent() != null && getIntent().getBooleanExtra("onlyInternet",false);
        currentSelected = getIntent() != null && getIntent().hasExtra("apps") ? getIntent().getStringArrayListExtra("apps") : new ArrayList<String>();
        infoTextWhitelist = getIntent() != null && getIntent().hasExtra("infoTextWhitelist") ? getIntent().getStringExtra("infoTextWhitelist") : null;
        infoTextBlacklist = getIntent() != null && getIntent().hasExtra("infoTextBlacklist") ? getIntent().getStringExtra("infoTextBlacklist") : null;
        whiteList = getIntent() != null && getIntent().getBooleanExtra("whitelist", false);
        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setHasFixedSize(true);
        ((SimpleItemAnimator) appList.getItemAnimator()).setSupportsChangeAnimations(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                listAdapter = new AppListAdapter();
                if(appList != null)runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appList.setAdapter(listAdapter);
                    }
                });
                else {
                    listAdapter.update = false;
                    listAdapter.apps.clear();
                    listAdapter.searchedApps.clear();
                    listAdapter = null;
                }
            }
        }).start();
        appList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 30) {
                    fabSettings.hide();
                } else if (dy < 5) fabSettings.show();
            }
        });
        ColorStateList stateList = ColorStateList.valueOf(ThemeHandler.getColor(this, R.attr.inputElementColor, Color.WHITE));
        final int textColor = ThemeHandler.getColor(this, android.R.attr.textColor, Color.BLACK);
        fabSettings.setBackgroundTintList(stateList);
        fabSettings.setCompatElevation(4);
        fabSettings.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(this, R.drawable.ic_settings), textColor));
        fabSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
        getSupportActionBar().setSubtitle(getString(R.string.x_apps_selected).replace("[[x]]", currentSelected.size() + ""));
    }

    private void showSettingsDialog(){
        View dialogContent = getLayoutInflater().inflate(R.layout.dialog_app_selection_settings, null, false);
        final CheckBox showSystem = dialogContent.findViewById(R.id.show_system_apps),
                showOnlyInternet = dialogContent.findViewById(R.id.only_show_apps_with_internet);
        showSystem.setChecked(showSystemApps);
        showOnlyInternet.setChecked(onlyInternet);
        new AlertDialog.Builder(this, ThemeHandler.getDialogTheme(this))
                .setTitle(R.string.settings).setView(dialogContent).setNeutralButton(R.string.cancel, null).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showSystemApps = showSystem.isChecked();
                onlyInternet = showOnlyInternet.isChecked();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(listAdapter != null) listAdapter.reload();
                    }
                }).start();
            }
        }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_autopause_appselect, menu);
        ((SearchView) menu.findItem(R.id.action_search).getActionView()).setOnQueryTextListener(this);
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
    protected void onDestroy() {
        super.onDestroy();
        appList.setAdapter(null);
        if(listAdapter != null){
            listAdapter.update = false;
            listAdapter.apps.clear();
            listAdapter.searchedApps.clear();
            listAdapter = null;
        }
        appList = null;
        fabSettings = null;
    }

    @Override
    protected Configuration getConfiguration() {
        return Configuration.withDefaults();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done){
            setResult(RESULT_OK, new Intent().putExtra("apps", currentSelected).putExtra("whitelist", whiteList));
            finish();
        }else if(item.getItemId() == android.R.id.home) {
            if (System.currentTimeMillis() - lastBackPress <= 1500 || !changed) {
                setResult(RESULT_CANCELED);
                finish();
            }else {
                lastBackPress = System.currentTimeMillis();
                Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_LONG).show();
            }
        }else return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(listAdapter != null && findViewById(R.id.progress).getVisibility() != View.VISIBLE)listAdapter.filter(newText);
        return true;
    }

    private final class AppListAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final TreeSet<AppEntry> apps = new TreeSet<>();
        private final List<AppEntry> searchedApps = new ArrayList<>();
        private boolean update = true;
        private String currentSearch = "";

        AppListAdapter() {
            reload();
        }

        void reload(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.progress).setVisibility(View.VISIBLE);
                }
            });
            apps.clear();
            List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
            AppEntry entry;
            for (ApplicationInfo packageInfo : packages) {
                entry = new AppEntry(AppSelectionActivity.this, packageInfo);
                if(!onlyInternet || entry.hasPermission(Manifest.permission.INTERNET)){
                    if(showSystemApps || !entry.isSystemApp())apps.add(entry);
                }
            }
            filter(currentSearch);
        }

        public void filter(String search){
            this.currentSearch = search;
            searchedApps.clear();
            if(search.equals("")){
                searchedApps.addAll(apps);
            }else{
                for(AppEntry entry: apps){
                    if(entry.getTitle().toLowerCase().contains(search.toLowerCase()))searchedApps.add(entry);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                    findViewById(R.id.progress).setVisibility(View.GONE);
                }
            });
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder((RelativeLayout) getLayoutInflater().inflate(viewType == 0 ?
                    R.layout.row_appselect_info : R.layout.row_app_entry, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            if(!update)return;
            if (holder.type == 0){
                ((TextView)holder.itemView.findViewById(R.id.text)).setText(whiteList ? infoTextWhitelist : infoTextBlacklist);
            }else{
                int offSet = 1;
                AppEntry entry = searchedApps.get(position - offSet);
                CheckBox checkBox = holder.itemView.findViewById(R.id.app_selected_indicator);
                ((ImageView) holder.itemView.findViewById(R.id.app_image)).setImageDrawable(entry.getIcon());
                ((TextView) holder.itemView.findViewById(R.id.app_title)).setText(entry.getTitle());
                holder.itemView.setClickable(true);
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(currentSelected.contains(entry.getPackageName()));
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CheckBox) v.findViewById(R.id.app_selected_indicator)).toggle();
                    }
                });
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if(!update)return;
                        if (isChecked) currentSelected.add(holder.appEntry.get().getPackageName());
                        else currentSelected.remove(holder.appEntry.get().getPackageName());
                        listAdapter.notifyItemChanged(0);
                        getSupportActionBar().setSubtitle(getString(R.string.x_apps_selected).replace("[[x]]", currentSelected.size() + ""));
                        changed = true;
                    }
                });
                holder.appEntry = new SoftReference<>(entry);
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
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        private SoftReference<AppEntry> appEntry;
        private final int type;

        ViewHolder(RelativeLayout layout, int type) {
            super(layout);
            this.type = type;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            appEntry.clear();
            appEntry = null;
        }
    }

    private static class AppEntry implements Comparable<AppEntry> {
        private final ApplicationInfo info;
        private SoftReference<Context> context;

        AppEntry(Context context, ApplicationInfo info) {
            this.info = info;
            this.context = new SoftReference<>(context);
        }

        public ApplicationInfo getRawInfo() {
            return info;
        }

        public String getTitle() {
            if(context.isEnqueued())
                throw new IllegalStateException("The Context supplied to the AppEntry doesn't exist anymore");
            return context.get().getPackageManager().getApplicationLabel(info).toString();
        }

        public String getPackageName(){
            return info.packageName;
        }

        public boolean isSystemApp() {
            return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }

        private Drawable getIcon() {
            if(context.isEnqueued())
                throw new IllegalStateException("The Context supplied to the AppEntry doesn't exist anymore");
            return info.loadIcon(context.get().getPackageManager());
        }

        @Override
        public int compareTo(@NonNull AppEntry o) {
            return getTitle().compareTo(o.getTitle());
        }

        public boolean hasPermission(String s){
            try {
                PackageInfo info = context.get().getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
                String[] permissions = info.requestedPermissions;
                if(permissions == null)return false;
                for(int i = 0; i < permissions.length; i++){
                    if(info.requestedPermissions[i].equals(s) && isPermissionGranted(info, i))return true;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean isPermissionGranted(PackageInfo info, String permission){
            for(int i = 0; i < info.requestedPermissions.length; i++){
                if(info.requestedPermissions[i].equals(permission)) {
                    return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || (info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                }
            }
            return false;
        }

        private boolean isPermissionGranted(PackageInfo info, int pos){
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || (info.requestedPermissionsFlags[pos] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
        }
    }
}
