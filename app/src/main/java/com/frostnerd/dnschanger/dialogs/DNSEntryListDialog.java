package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.DNSEntryAdapter;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.utils.adapters.DatabaseAdapter;
import com.frostnerd.utils.database.orm.parser.ParsedEntity;
import com.frostnerd.utils.database.orm.parser.columns.Column;
import com.frostnerd.utils.database.orm.statementoptions.queryoptions.WhereCondition;
import com.frostnerd.utils.lifecyclehelper.UtilityDialog;
import java.util.HashSet;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 * <p>
 * <p>
 * development@frostnerd.com
 */
public class DNSEntryListDialog extends UtilityDialog {
    private OnProviderSelectedListener listener;
    private RecyclerView list;
    private DNSEntryAdapter adapter;
    private DNSEntryAdapter.OnEntrySelected entrySelected = new DNSEntryAdapter.OnEntrySelected() {
        @Override
        public void selected(DNSEntry entry) {
            listener.onProviderSelected(entry.getName(), entry.getDns1(), entry.getDns2(), entry.getDns1V6(), entry.getDns2V6());
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
                                public void onCreationFinished(String name, IPPortPair dns1, IPPortPair dns2, IPPortPair dns1V6, IPPortPair dns2V6) {
                                    DatabaseHelper.getInstance(getContext()).insert(new DNSEntry(name, name, dns1,
                                            dns2, dns1V6, dns2V6, "", true));
                                    adapter.reloadData();
                                }
                            }).show();
                        }else {
                            new DNSCreationDialog(getContext(), new DNSCreationDialog.OnEditingFinishedListener() {
                                @Override
                                public void editingFinished(DNSEntry entry) {
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
        if(adapter != null)adapter.cleanup();
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
