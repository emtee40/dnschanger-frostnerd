package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.frostnerd.database.DatabaseAdapter;
import com.frostnerd.database.orm.parser.ParsedEntity;
import com.frostnerd.database.orm.parser.columns.Column;
import com.frostnerd.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.DNSEntryAdapter;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.lifecycle.BaseDialog;

import java.util.HashSet;
import java.util.Set;

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
public class DNSEntryListDialog extends BaseDialog {
    private OnProviderSelectedListener listener;
    private RecyclerView list;
    private DNSEntryAdapter adapter;
    private DNSEntryAdapter.OnEntrySelected entrySelected = new DNSEntryAdapter.OnEntrySelected() {
        @Override
        public void selected(DNSEntry entry) {
            listener.onProviderSelected(entry.getName(), entry.getDns1(), entry.getDns2(), entry.getDns1V6(), entry.getDns2V6());
            dismiss();
        }
    };
    private Set<Long> selectedEntries = new HashSet<>();

    public DNSEntryListDialog(@NonNull final Context context, final int theme, @NonNull final OnProviderSelectedListener listener) {
        super(context, theme);
        prepareAdapter();
        this.listener = listener;
        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_default_dns, null, false);
        setView(layout);
        list = layout.findViewById(R.id.defaultDnsDialogList);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter);
        list.setHasFixedSize(true);
        setTitle(R.string.default_dns_title);
        prepareButtons();
        setButton(BUTTON_NEUTRAL, context.getString(R.string.remove), (OnClickListener) null);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setButton(BUTTON_POSITIVE, context.getString(R.string.add), (OnClickListener) null);
    }

    private void prepareButtons(){
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(selectedEntries.size() != 1){
                            new DNSCreationDialog(getContext(), new DNSCreationDialog.OnCreationFinishedListener() {
                                @Override
                                public void onCreationFinished(@NonNull String name, @NonNull IPPortPair dns1, IPPortPair dns2, @NonNull IPPortPair dns1V6, IPPortPair dns2V6) {
                                    DatabaseHelper.getInstance(getContext()).insert(new DNSEntry(name, name, dns1,
                                            dns2, dns1V6, dns2V6, "", true));
                                    adapter.reloadData();
                                }
                            }).show();
                        }else {
                            //noinspection ConstantConditions
                            new DNSCreationDialog(getContext(), new DNSCreationDialog.OnEditingFinishedListener() {
                                @Override
                                public void editingFinished(@NonNull DNSEntry entry) {
                                    DatabaseHelper.getInstance(getContext()).update(entry);
                                    adapter.reloadData();
                                }
                            }, DatabaseHelper.getInstance(getContext()).getByRowID(DNSEntry.class, (Long) selectedEntries.toArray()[0])).show();
                        }
                    }
                });
                getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                getButton(BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DatabaseHelper helper = DatabaseHelper.getInstance(getContext());
                        for(long l: selectedEntries){
                            helper.delete(DNSEntry.class, WhereCondition.equal("id", String.valueOf(l)));
                        }
                        adapter.reloadData();
                        v.setVisibility(View.INVISIBLE);
                        getButton(BUTTON_POSITIVE).setText(R.string.add);
                    }
                });
            }
        });
    }

    private void prepareAdapter(){
        final boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(getContext()), ipv6Enabled = !ipv4Enabled || PreferencesAccessor.isIPv6Enabled(getContext());
        adapter = new DNSEntryAdapter(getContext(), entrySelected);
        if(!ipv4Enabled || !ipv6Enabled) adapter.filter(new DatabaseAdapter.ArgumentLessFilter() {
            private final WhereCondition condition;
            {
                ParsedEntity<DNSEntry> parsedEntity = ParsedEntity.wrapEntity(DNSEntry.class);
                Column<DNSEntry> column = parsedEntity.getTable().findColumn(ipv4Enabled ? "dns1" : "dns1v6");
                condition = WhereCondition.notNull(column).and(WhereCondition.equal(column, "").not());
            }

            @Override
            public WhereCondition getCondition() {
                return condition;
            }

            @Override
            public boolean isResourceIntensive() {
                return false;
            }

            @Override
            public DatabaseAdapter.Filter[] exclusiveWith() {
                return new DatabaseAdapter.Filter[0];
            }
        });
        adapter.setOnEntrySelectionUpdated(new DNSEntryAdapter.OnEntrySelectionUpdated() {
            @Override
            public void selectionUpdated(Set<Long> to) {
                selectedEntries = to;
                if(selectedEntries.size() == 0){
                    getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                    getButton(BUTTON_POSITIVE).setText(R.string.add);
                } else {
                    if (selectedEntries.size() == 1)
                        getButton(BUTTON_POSITIVE).setText(R.string.edit);
                    else
                        getButton(BUTTON_POSITIVE).setText(R.string.add);
                    getButton(BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void destroy(){
        if(adapter != null)adapter.destroy();
        listener = null;
        entrySelected = null;
        selectedEntries = null;
        list.setAdapter(null);
        list = null;
    }

    public interface OnProviderSelectedListener {
        void onProviderSelected(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6);
    }
}
