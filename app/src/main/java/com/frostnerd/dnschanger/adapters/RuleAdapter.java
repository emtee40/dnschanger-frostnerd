package com.frostnerd.dnschanger.adapters;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostnerd.database.DatabaseAdapter;
import com.frostnerd.database.orm.parser.columns.Column;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSRule;
import com.frostnerd.dnschanger.dialogs.NewRuleDialog;
import com.frostnerd.dnschanger.util.RuleImport;
import com.frostnerd.lifecycle.BaseViewHolder;


/*
 * Copyright (C) 2019 Daniel Wolf (Ch4t4r)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * You can contact the developer at daniel.wolf@frostnerd.com.
 */
public class RuleAdapter<T extends Activity &RuleImport.ImportStartedListener> extends DatabaseAdapter<DNSRule, RuleAdapter.ViewHolder> {
    private LayoutInflater layoutInflater;
    private NewRuleDialog newRuleDialog;
    private T context;
    private static Column<DNSRule> ipv6Column;
    private static Column<DNSRule> hostColumn;
    private static Column<DNSRule> targetColumn;
    private static Column<DNSRule> wildcardColumn;
    private NewRuleDialog.CreationListener creationListener = new NewRuleDialog.CreationListener() {
        @Override
        public void creationFinished(@NonNull String host, @Nullable String target, @Nullable String targetV6, boolean ipv6, boolean wildcard, boolean wasEdited) {
            if (target != null)
                DatabaseHelper.getInstance(context).editDNSRule(host, ipv6, target);
            else {
                DatabaseHelper.getInstance(context).deleteDNSRule(host, ipv6);
            }
            newRuleDialog = null;
            reloadData();
        }
    };

    public RuleAdapter(T context, DatabaseHelper databaseHelper, final TextView rowCount, ProgressBar updateProgress){
        super(databaseHelper, 10000);
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
        setOnRowLoaded(new OnRowLoaded<DNSRule, ViewHolder>() {
            @Override
            public void bindRow(ViewHolder view, final DNSRule entity, int position) {
                view.host.setText(entity.getHost());
                view.target.setText(entity.getTarget());
                view.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        newRuleDialog = new NewRuleDialog(RuleAdapter.this.context, creationListener, entity.getHost(), entity.getTarget(), entity.isWildcard(), entity.isIpv6());
                        newRuleDialog.show();
                        return true;
                    }
                });
            }

            @Override
            public void bindNonEntityRow(ViewHolder view, int position) {

            }
        });
        setReloadCallback(new Runnable() {
            @Override
            public void run() {
                rowCount.setText(RuleAdapter.this.context.getString(R.string.x_entries).replace("[x]", getItemCount() + ""));
            }
        });
        setProgressView(updateProgress);
        setUpdateDataOnConfigChange(false);
        filter(ArgumentLessFilter.SHOW_NORMAL);
        filter(ArgumentLessFilter.SHOW_WILDCARD);
        filter(ArgumentLessFilter.SHOW_IPV6);
        filter(ArgumentLessFilter.SHOW_IPV4);
        ipv6Column = databaseHelper.findColumn(DNSRule.class, "ipv6");
        hostColumn = databaseHelper.findColumn(DNSRule.class, "host");
        targetColumn = databaseHelper.findColumn(DNSRule.class, "target");
        wildcardColumn = databaseHelper.findColumn(DNSRule.class, "wildcard");
        setUpdateDataOnConfigChange(true);
        reloadData();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(layoutInflater.inflate(R.layout.row_rule, parent, false));
    }

    static class ViewHolder extends BaseViewHolder {
        private TextView host, target;

        private ViewHolder(View itemView) {
            super(itemView);
            host = itemView.findViewById(R.id.text);
            target = itemView.findViewById(R.id.text3);
        }

        @Override
        protected void destroy() {
            host = target = null;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            host = null;
            target = null;
        }
    }

    @Override
    protected boolean beforeReload() {
        return true;
    }

    @Override
    protected void cleanup() {
        super.cleanup();
        context = null;
        layoutInflater = null;
        if(newRuleDialog != null)newRuleDialog.dismiss();
        newRuleDialog = null;
    }

    @Override
    public int queryDBCount() {
        return super.queryDBCount();
    }

    public enum ArgumentLessFilter implements DatabaseAdapter.ArgumentLessFilter{
        SHOW_IPV6 {
            @Override
            public WhereCondition getCondition() {
                return WhereCondition.equal(ipv6Column, "1");
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[]{SHOW_IPV4};
            }
        }, HIDE_LOCAL {
            @Override
            public WhereCondition getCondition() {
                return WhereCondition.equal(targetColumn, "127.0.0.1").not()
                        .and(WhereCondition.equal(targetColumn, "::1").not());
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[0];
            }
        }, SHOW_WILDCARD {
            @Override
            public WhereCondition getCondition() {
                return WhereCondition.equal(wildcardColumn, "1");
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[]{SHOW_NORMAL};
            }
        }, SHOW_IPV4 {
            @Override
            public WhereCondition getCondition() {
                return WhereCondition.equal(ipv6Column, "0");
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[]{SHOW_IPV4};
            }
        }, SHOW_NORMAL{
            @Override
            public WhereCondition getCondition() {
                return WhereCondition.equal(wildcardColumn, "1").not();
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[]{SHOW_WILDCARD};
            }
        }
    }

    public enum ArgumentBasedFilter implements ArgumentFilter{
        TARGET {
            @Override
            public WhereCondition getCondition(String argument) {
                return WhereCondition.like(targetColumn, "%" + argument + "%");
            }

            @Override
            public boolean isResourceIntensive() {
                return true;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[0];
            }
        }, HOST_SEARCH{
            @Override
            public WhereCondition getCondition(String argument) {
                return WhereCondition.like(hostColumn, "%" + argument + "%");
            }

            @Override
            public boolean isResourceIntensive() {
                return true;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[0];
            }
        }
    }
}
