package com.frostnerd.dnschanger.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.adapters.RuleAdapter;
import com.frostnerd.dnschanger.dialogs.NewRuleDialog;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.general.DesignUtil;
import com.frostnerd.utils.networking.NetworkUtil;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class RulesFragment extends Fragment implements SearchView.OnQueryTextListener {
    private View content;
    private RecyclerView list;
    private RuleAdapter ruleAdapter;
    private FloatingActionButton fabOpen;
    private boolean fabExpanded = false, wildcardShown = false;
    private View sqlWrap, newWrap, filterWrap;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return content = inflater.inflate(R.layout.fragment_rules, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fabOpen = content.findViewById(R.id.fab_open);
        list = content.findViewById(R.id.list);
        newWrap = content.findViewById(R.id.wrap_fab_new);
        filterWrap = content.findViewById(R.id.wrap_fab_filter);
        sqlWrap = content.findViewById(R.id.wrap_fab_sql);
        FloatingActionButton fabSQL = content.findViewById(R.id.fab_sql);
        FloatingActionButton fabNew = content.findViewById(R.id.fab_new);
        FloatingActionButton fabFilter = content.findViewById(R.id.fab_filter);

        ruleAdapter = new RuleAdapter((MainActivity)getContext(), Util.getDBHelper(getContext()),
                (TextView)content.findViewById(R.id.row_count), (ProgressBar)content.findViewById(R.id.progress));
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(ruleAdapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 30) {
                    fabExpanded = false;
                    fabOpen.hide();
                    fabOpen.setRotation(0);
                    newWrap.setAlpha(0);
                    filterWrap.setAlpha(0);
                    sqlWrap.setAlpha(0);
                } else if (dy < 30) fabOpen.show();
            }
        });
        ColorStateList stateList = ColorStateList.valueOf(ThemeHandler.getColor(getContext(), R.attr.inputElementColor, Color.WHITE));
        final int textColor = ThemeHandler.getColor(getContext(), android.R.attr.textColor, Color.BLACK);
        fabNew.setBackgroundTintList(stateList);
        fabOpen.setBackgroundTintList(stateList);
        fabSQL.setBackgroundTintList(stateList);
        fabFilter.setBackgroundTintList(stateList);
        fabOpen.setCompatElevation(4);
        fabSQL.setCompatElevation(4);
        fabNew.setCompatElevation(8);
        fabFilter.setCompatElevation(4);
        fabOpen.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(getContext(), R.drawable.ic_settings), textColor));
        fabNew.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(getContext(), R.drawable.ic_add), textColor));
        fabSQL.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(getContext(), R.drawable.ic_chart), textColor));
        fabFilter.setImageDrawable(DesignUtil.setDrawableColor(DesignUtil.getDrawable(getContext(), R.drawable.ic_filter), textColor));

        fabOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabExpanded = !fabExpanded;
                animateFab();
            }
        });
        fabSQL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        fabNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new NewRuleDialog((MainActivity)getContext(), new NewRuleDialog.CreationListener() {
                    @Override
                    public void creationFinished(@NonNull String host, @NonNull String target, @Nullable String targetV6, boolean ipv6, boolean wildcard, boolean editingMode) {
                        boolean both = targetV6 != null && !targetV6.equals("");
                        Util.getDBHelper(getContext()).createDNSRule(host, target, !both && ipv6, wildcard);
                        if (targetV6 != null && !targetV6.equals("")) {
                            Util.getDBHelper(getContext()).createDNSRule(host, targetV6, true, wildcard);
                        }
                        if(wildcard == wildcardShown){
                            list.scrollTo(0, 0);
                            ruleAdapter.reloadData();
                        }
                    }
                }).show();
            }
        });
        fabFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterDialog();
            }
        });
        int inputColor = ThemeHandler.getColor(getContext(), R.attr.inputElementColor, -1);
        content.findViewById(R.id.text2).setBackgroundColor(inputColor);
        content.findViewById(R.id.text).setBackgroundColor(inputColor);
        content.findViewById(R.id.text3).setBackgroundColor(inputColor);
    }

    private void showFilterDialog() {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_rule_filter, null, false);
        final RadioButton ipv4 = dialog.findViewById(R.id.radio_ipv4), ipv6 = dialog.findViewById(R.id.radio_ipv6),
                both = dialog.findViewById(R.id.radio_both);
        final CheckBox showLocal = dialog.findViewById(R.id.show_local),
            showNormal = dialog.findViewById(R.id.show_normal), showWildcard = dialog.findViewById(R.id.show_wildcard);
        final EditText targetSearch = dialog.findViewById(R.id.target);
        final MaterialEditText metTarget = dialog.findViewById(R.id.met_target);
        if(ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_IPV6) && ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_IPV4))
            both.setChecked(true);
        else if (ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_IPV6))
            ipv6.setChecked(true);
        else if (ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_IPV4))
            ipv4.setChecked(true);
        if (ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.HIDE_LOCAL))
            showLocal.setChecked(false);
        if (ruleAdapter.hasFilter(RuleAdapter.ArgumentBasedFilter.TARGET))
            targetSearch.setText(ruleAdapter.getFilterValue(RuleAdapter.ArgumentBasedFilter.TARGET));
        if (!ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_NORMAL))
            showNormal.setChecked(false);
        if (!ruleAdapter.hasFilter(RuleAdapter.ArgumentLessFilter.SHOW_WILDCARD))
            showWildcard.setChecked(false);

        new AlertDialog.Builder(getContext()).setTitle(R.string.filter).setCancelable(false).setView(dialog).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ruleAdapter.setUpdateDataOnConfigChange(false);
                if (both.isChecked()){
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_IPV4);
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_IPV6);
                }else if (ipv4.isChecked()){
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_IPV4);
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentLessFilter.SHOW_IPV6);
                }else{
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_IPV6);
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentLessFilter.SHOW_IPV4);
                }

                if(!showLocal.isChecked()){
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.HIDE_LOCAL);
                }else{
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentLessFilter.HIDE_LOCAL);
                }

                if (metTarget.getIndicatorState() == MaterialEditText.IndicatorState.UNDEFINED && !targetSearch.getText().toString().equals("")){
                    ruleAdapter.filter(RuleAdapter.ArgumentBasedFilter.TARGET, targetSearch.getText().toString());
                }else{
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentBasedFilter.TARGET);
                }

                if(showNormal.isChecked()){
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_NORMAL);
                }else{
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentLessFilter.SHOW_NORMAL);
                }

                if(showWildcard.isChecked()){
                    wildcardShown = true;
                    ruleAdapter.filter(RuleAdapter.ArgumentLessFilter.SHOW_WILDCARD);
                }else{
                    wildcardShown = false;
                    ruleAdapter.removeFilters(RuleAdapter.ArgumentLessFilter.SHOW_WILDCARD);
                }

                ruleAdapter.setUpdateDataOnConfigChange(true);
                list.scrollTo(0, 0);
                ruleAdapter.reloadData();
            }
        }).show();
        targetSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.equals("") || NetworkUtil.isIP(s.toString(), false) || NetworkUtil.isIP(s.toString(), true)){
                    metTarget.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                }else metTarget.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void animateFab() {
        ViewPropertyAnimatorCompat anim = ViewCompat.animate(fabOpen).rotation(fabExpanded ? 135f : -135f).withLayer().
                setDuration(300).setInterpolator(new OvershootInterpolator());
        ViewPropertyAnimatorCompat anim2 = ViewCompat.animate(newWrap).alpha(fabExpanded ? 1.0f : 0f).setDuration(300);
        ViewPropertyAnimatorCompat anim3 = ViewCompat.animate(sqlWrap).alpha(fabExpanded ? 1.0f : 0f).setDuration(300);
        ViewPropertyAnimatorCompat anim4 = ViewCompat.animate(filterWrap).alpha(fabExpanded ? 1.0f : 0f).setDuration(300);
        anim.start();
        anim2.start();
        anim3.start();
        anim4.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_rules, menu);

        SearchManager searchManager = (SearchManager) getContext().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(Util.getActivity(this).getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        ruleAdapter.filter(RuleAdapter.ArgumentBasedFilter.HOST_SEARCH, query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if(newText.equals("")){
            ruleAdapter.removeFilters(RuleAdapter.ArgumentBasedFilter.HOST_SEARCH);
            return true;
        }
        return false;
    }

    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context == null ? MainActivity.currentContext : context;
    }

    public RuleAdapter getRuleAdapter() {
        return ruleAdapter;
    }
}
