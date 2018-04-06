package com.frostnerd.dnschanger.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.DatabaseHelper;
import com.frostnerd.dnschanger.database.entities.DNSEntry;
import com.frostnerd.utils.adapters.DatabaseAdapter;
import com.frostnerd.utils.general.DesignUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Copyright Daniel Wolf 2018
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class DNSEntryAdapter extends DatabaseAdapter<DNSEntry, DNSEntryAdapter.ViewHolder> {
    private Context context;
    private LayoutInflater layoutInflater;
    private Set<Long> selectedEntries = new HashSet<>();
    @NonNull private OnEntrySelected entrySelected;
    private OnEntrySelectionUpdated onEntrySelectionUpdated;
    private static final int idTagKey = R.string.active, positionTagKey = R.string.app_name;
    private View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            toggleEntrySelection(v);
            return true;
        }
    };
    private View.OnClickListener clickListener = new View.OnClickListener() {
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

        long id = (Long) v.getTag(idTagKey);
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

    @Override
    public void reloadData() {
        selectedEntries.clear();
        super.reloadData();
    }

    public DNSEntryAdapter(@NonNull Context context, @NonNull OnEntrySelected entrySelected) {
        super(DatabaseHelper.getInstance(context), 10000);
        this.entrySelected = entrySelected;
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        setOnRowLoaded(new OnRowLoaded<DNSEntry, ViewHolder>() {
            @Override
            public void bindRow(ViewHolder view, DNSEntry entity, int position) {
                boolean wasSelected = view.itemView.isSelected();
                if(wasSelected && !selectedEntries.contains(entity.getID())) {
                    view.itemView.setSelected(false);
                    view.itemView.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, android.R.attr.windowBackground));
                } else if(!wasSelected && selectedEntries.contains(entity.getID())) {
                    view.itemView.setSelected(true);
                    view.itemView.setBackgroundColor(DesignUtil.resolveColor(DNSEntryAdapter.this.context, R.attr.inputElementColor));
                }
                view.textView.setText(entity.getName());
                if(TextUtils.isEmpty(entity.getDescription())){
                    if(view.subText.getVisibility() == View.VISIBLE) view.subText.setVisibility(View.GONE);
                } else {
                    view.subText.setText(entity.getDescription());
                    if(view.subText.getVisibility() != View.VISIBLE) view.subText.setVisibility(View.VISIBLE);
                }

                view.itemView.setTag(idTagKey, entity.getID());
                view.itemView.setTag(positionTagKey, position);
            }

            @Override
            public void bindNonEntityRow(ViewHolder view, int position) {
                if(view.getItemViewType() == 1){
                    view.textView.setText(R.string.default_dns_explain_delete);
                }
            }
        });
        reloadData();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == 1 ? R.layout.row_text_cardview : R.layout.item_default_dns;
        View itemView = layoutInflater.inflate(layout, parent, false);
        itemView.setOnLongClickListener(longClickListener);
        itemView.setOnClickListener(clickListener);

        return new ViewHolder(itemView, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 1 : 0;
    }

    @Override
    public int getDefaultViewType() {
        return 0;
    }

    @Override
    public void cleanup() {
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
        this.onEntrySelectionUpdated = onEntrySelectionUpdated;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView textView, subText;

        private ViewHolder(View itemView, int type) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            if(type== 0)subText = itemView.findViewById(R.id.text2);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            textView = subText = null;
        }
    }

    public interface OnEntrySelected {
        public void selected(DNSEntry entry);
    }

    public interface OnEntrySelectionUpdated {
        public void selectionUpdated(Set<Long> to);
    }
}
