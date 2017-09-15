package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.DNSEntry;
import com.frostnerd.dnschanger.R;
import com.frostnerd.utils.networking.NetworkUtil;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
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
public class DefaultDNSDialog extends AlertDialog {
    View layout;
    private OnProviderSelectedListener listener;
    private List<DNSEntry> localEntries = new ArrayList<>();
    private DefaultDNSAdapter adapter;
    private boolean removeButtonShown;
    private RecyclerView list;
    private List<DNSEntry> removal = new ArrayList<>();

    public DefaultDNSDialog(@NonNull final Context context, final int theme, @NonNull final OnProviderSelectedListener listener) {
        super(context, theme);
        localEntries = API.getDBHelper(context).getDNSEntries();
        boolean ipv4Enabled = API.isIPv4Enabled(context), ipv6Enabled = !ipv4Enabled || API.isIPv6Enabled(context);
        List<DNSEntry> tmp = new ArrayList<>();
        if(!ipv4Enabled || !ipv6Enabled){
            for(DNSEntry entry: localEntries){
                if((!ipv4Enabled && NetworkUtil.isAssignableAddress(entry.getDns1V6(), true)) ||
                        (!ipv6Enabled && NetworkUtil.isAssignableAddress(entry.getDns1(), false))){
                    tmp.add(entry);
                }
            }
            localEntries = tmp;
        }
        this.listener = listener;
        layout = LayoutInflater.from(context).inflate(R.layout.dialog_default_dns, null, false);
        setView(layout);
        list = (RecyclerView) layout.findViewById(R.id.defaultDnsDialogList);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter = new DefaultDNSAdapter());
        list.setHasFixedSize(true);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setButton(BUTTON_POSITIVE, context.getString(R.string.add), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        setTitle(R.string.default_dns_title);
        setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(removal.size() != 1){
                            new DNSCreationDialog(context, new DNSCreationDialog.OnCreationFinishedListener() {
                                @Override
                                public void onCreationFinished(String name, String dns1, String dns2, String dns1V6, String dns2V6) {
                                    API.getDBHelper(context).saveDNSEntry(new DNSEntry(0,name, dns1, dns2, dns1V6, dns2V6, "",true));
                                    localEntries.clear();
                                    localEntries = API.getDBHelper(context).getDNSEntries();
                                    list.setAdapter(adapter = new DefaultDNSAdapter());
                                }
                            }).show();
                        }else{
                            new DNSCreationDialog(context, new DNSCreationDialog.OnEditingFinishedListener() {
                                @Override
                                public void editingFinished(DNSEntry entry) {
                                    API.getDBHelper(context).editEntry(entry);
                                    localEntries.clear();
                                    localEntries = API.getDBHelper(context).getDNSEntries();
                                    list.setAdapter(adapter = new DefaultDNSAdapter());
                                }
                            }, removal.get(0)).show();
                            removal.clear();
                            removeButtonShown = false;
                            getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                            getButton(BUTTON_POSITIVE).setText(R.string.add);
                        }
                    }
                });
                getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                getButton(BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeButtonShown = false;
                        v.setVisibility(View.INVISIBLE);
                        for(DNSEntry entry: removal){
                            API.getDBHelper(context).removeDNSEntry(entry.getID());
                            localEntries.remove(entry);
                        }
                        removal.clear();
                        list.setAdapter(adapter = new DefaultDNSAdapter());
                        getButton(BUTTON_POSITIVE).setText(R.string.add);
                    }
                });
            }
        });
        setButton(BUTTON_NEUTRAL, context.getString(R.string.remove), (OnClickListener)null);
    }

    public static interface OnProviderSelectedListener{
        public void onProviderSelected(String name, String dns1, String dns2, String dns1V6, String dns2V6);
    }

    private class DefaultDNSAdapter extends RecyclerView.Adapter<DefaultDNSAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder{
            private View layout;
            private int type;

            public ViewHolder(View itemView, int type) {
                super(itemView);
                this.layout = itemView;
                this.type = type;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(viewType == 0 ? R.layout.row_text_cardview : R.layout.item_default_dns, parent, false),viewType);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if(holder.type == 0){
                ((TextView) holder.layout.findViewById(R.id.text)).setText(getContext().getString(R.string.default_dns_explain_delete));
            }else{
                holder.layout.setSelected(false);
                ((TextView) holder.layout.findViewById(R.id.text)).setText(localEntries.get(position).getName());
                holder.layout.setLongClickable(true);
                holder.layout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        v.setSelected(!v.isSelected());
                        v.setBackgroundColor(v.isSelected() ? API.resolveColor(getContext(), R.attr.inputElementColor) : API.resolveColor(getContext(), android.R.attr.windowBackground));
                        if(!removeButtonShown){
                            getButton(BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                            getButton(BUTTON_POSITIVE).setText(R.string.edit);
                            removeButtonShown = true;
                        }
                        if(!v.isSelected()){
                            if(removal.size() == 2)getButton(BUTTON_POSITIVE).setText(R.string.add);
                            removal.remove((DNSEntry)v.getTag());
                        }
                        else{
                            if(removal.size() == 1)getButton(BUTTON_POSITIVE).setText(R.string.add);
                            removal.add((DNSEntry) v.getTag());
                        }
                        if(removal.size() == 0){
                            removeButtonShown = false;
                            getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                            getButton(BUTTON_POSITIVE).setText(R.string.add);
                        }
                        return true;
                    }
                });
                holder.layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(removeButtonShown){
                            v.setSelected(!v.isSelected());
                            v.setBackgroundColor(v.isSelected() ? API.resolveColor(getContext(), R.attr.inputElementColor) : API.resolveColor(getContext(), android.R.attr.windowBackground));
                            if(!v.isSelected()){
                                if(removal.size() == 2)getButton(BUTTON_POSITIVE).setText(R.string.edit);
                                removal.remove((DNSEntry)v.getTag());
                            }
                            else{
                                if(removal.size() == 1)getButton(BUTTON_POSITIVE).setText(R.string.add);
                                removal.add((DNSEntry) v.getTag());
                            }
                            if(removal.size() == 0){
                                removeButtonShown = false;
                                getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                                getButton(BUTTON_POSITIVE).setText(R.string.add);
                            }
                        }else{
                            dismiss();
                            DNSEntry entry = (DNSEntry)v.getTag();
                            listener.onProviderSelected(entry.getName(), entry.getDns1(), entry.getDns2(), entry.getDns1V6(), entry.getDns2V6());
                        }
                    }
                });
                if(localEntries.get(position).getDescription().equals(""))holder.layout.findViewById(R.id.text2).setVisibility(View.GONE);
                else ((TextView)holder.layout.findViewById(R.id.text2)).setText(localEntries.get(position).getDescription());
                holder.layout.setTag(localEntries.get(position));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return localEntries.size();
        }
    }
}
