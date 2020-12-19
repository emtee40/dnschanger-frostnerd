package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.frostnerd.database.DatabaseAdapter;
import com.frostnerd.database.orm.statementoptions.queryoptions.OrderOption;
import com.frostnerd.design.DesignUtil;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.lifecycle.BaseViewHolder;

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
public class DNSEntryAdapter extends DatabaseAdapter<DNSEntry, DNSEntryAdapter.ViewHolder> {
    @NonNull private Context context;
    @NonNull private LayoutInflater layoutInflater;
    @NonNull private Set<Long> selectedEntries = new HashSet<>();
    @NonNull private OnEntrySelected entrySelected;
    private OnEntrySelectionUpdated onEntrySelectionUpdated;
    private static final int idTagKey = R.string.active, positionTagKey = R.string.app_name;
    @NonNull private View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            toggleEntrySelection(v);
            return true;
        }
    };
    @NonNull private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(selectedEntries.size() == 0){
                entrySelected.selected(getEntityAtPosition((Integer)v.getTag(positionTagKey)));
            } else {
                toggleEntrySelection(v);
            }
        }
    };

    private void toggleEntrySelection(View v){
        if(onEntrySelectionUpdated == null)return;
        boolean wasSelected = v.isSelected();

        Object tag = v.getTag(idTagKey);
        if (tag != null) {
            long id = (Long) tag;
            if(selectedEntries.contains(id)) {
                selectedEntries.remove(id);
                v.setSelected(false);
            } else{
                selectedEntries.add(id);
                v.setSelected(true);
            }

            if(wasSelected && !selectedEntries.contains(id)) {
                v.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, android.R.attr.windowBackground));
            } else if(!wasSelected && selectedEntries.contains(id)) {
                v.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, R.attr.inputElementColor));
            }

            onEntrySelectionUpdated.selectionUpdated(selectedEntries);
        }
    }

    @Override
    protected boolean beforeReload() {
        selectedEntries.clear();
        if(onEntrySelectionUpdated != null) onEntrySelectionUpdated.selectionUpdated(selectedEntries);
        return true;
    }

    public DNSEntryAdapter(@NonNull Context context, @NonNull OnEntrySelected entrySelected) {
        super(DatabaseHelper.getInstance(context), 10000);
        this.entrySelected = entrySelected;
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        boolean ipv6 = PreferencesAccessor.isIPv6Enabled(context);
        boolean ipv4 = PreferencesAccessor.isIPv4Enabled(context);
        IPPortPair _primary = ipv4 ? PreferencesAccessor.Type.DNS1.getPair(context) : null,
                _secondary = ipv4 ? PreferencesAccessor.Type.DNS2.getPair(context) : null,
                _primaryV6 = ipv6 ? PreferencesAccessor.Type.DNS1_V6.getPair(context) : null,
                _secondaryV6 = ipv6 ? PreferencesAccessor.Type.DNS2_V6.getPair(context) : null;
        setOnRowLoaded(new OnRowLoaded<DNSEntry, ViewHolder>() {
            @Override
            public void bindRow(ViewHolder view, DNSEntry entity, int position) {
                boolean wasSelected = view.itemView.isSelected();
                boolean isCurrentServer = ipMatches(ipv4 ? entity.getDns1() : null, _primary) && ipMatches(ipv4 ? entity.getDns2() : null, _secondary) &&
                        ipMatches(ipv6 ? entity.getDns1V6() : null, _primaryV6) && ipMatches(ipv6 ? entity.getDns2V6() : null, _secondaryV6);

                if(wasSelected && !selectedEntries.contains(entity.getID())) {
                    view.itemView.setSelected(false);
                    view.itemView.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, android.R.attr.windowBackground));
                } else if(!wasSelected && selectedEntries.contains(entity.getID())) {
                    view.itemView.setSelected(true);
                    view.itemView.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, R.attr.inputElementColor));
                }
                view.textView.setText(entity.getName());
                if(view.subText != null) {
                    if(TextUtils.isEmpty(entity.getDescription())){
                        if(view.subText.getVisibility() == View.VISIBLE) view.subText.setVisibility(View.GONE);
                    } else {
                        view.subText.setText(entity.getDescription());
                        if(view.subText.getVisibility() != View.VISIBLE) view.subText.setVisibility(View.VISIBLE);
                    }
                }

                if(view.itemView.isLongClickable() && onEntrySelectionUpdated == null){
                    view.itemView.setOnLongClickListener(null);
                    view.itemView.setLongClickable(false);
                }else if(onEntrySelectionUpdated != null && !view.itemView.isLongClickable()){
                    view.itemView.setOnLongClickListener(longClickListener);
                }
                view.radioButton.setChecked(isCurrentServer);

                view.itemView.setOnClickListener(clickListener);
                view.itemView.setTag(idTagKey, entity.getID());
                view.itemView.setTag(positionTagKey, position);
            }

            @Override
            public void bindNonEntityRow(ViewHolder view, int position) {
                if(view.getItemViewType() == 1){
                    view.textView.setText(R.string.default_dns_explain_delete);
                    view.textView.setOnClickListener(null);
                    view.itemView.setOnClickListener(null);
                }
            }
        });
        OrderOption order = new OrderOption().order("customEntry", false)
                .order("name", true, OrderOption.Collate.NOCASE);
        setOrderOption(order);
        reloadData();
    }

    private boolean ipMatches(IPPortPair one, IPPortPair two) {
        if((one == null || one.isEmpty()) && (two == null || two.isEmpty())) return true;
        else if(one == null || two == null) return false;
        return one.matches(two);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 1 ? R.layout.row_text_cardview : R.layout.item_default_dns;
        View itemView = layoutInflater.inflate(layout, parent, false);
        if(onEntrySelectionUpdated != null)itemView.setOnLongClickListener(longClickListener);
        else {
            itemView.setLongClickable(false);
            itemView.setOnLongClickListener(null);
        }
        itemView.setOnClickListener(clickListener);
        return new ViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 1 : 0;
    }

    @Override
    public int getDefaultViewType() {
        return 0;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void cleanup() {
        super.cleanup();
        context = null;
        layoutInflater = null;
        selectedEntries = null;
        entrySelected = null;
        onEntrySelectionUpdated = null;
        longClickListener = null;
        clickListener = null;
    }

    public void setOnEntrySelectionUpdated(OnEntrySelectionUpdated onEntrySelectionUpdated) {
        boolean wasNull = this.onEntrySelectionUpdated == null;
        this.onEntrySelectionUpdated = onEntrySelectionUpdated;
        if(wasNull) notifyDataSetChanged();
    }

    static class ViewHolder extends BaseViewHolder {
        private TextView textView, subText;
        private RadioButton radioButton;

        private ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            subText = itemView.findViewById(R.id.text2);
            radioButton = itemView.findViewById(R.id.radioButton);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

        @Override
        protected void destroy() {

        }
    }

    public interface OnEntrySelected {
        void selected(DNSEntry entry);
    }

    public interface OnEntrySelectionUpdated {
        void selectionUpdated(Set<Long> to);
    }
}
