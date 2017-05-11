package com.frostnerd.dnschanger.dialogs;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.R;

import java.util.ArrayList;
import java.util.List;


public class DefaultDNSDialog extends AlertDialog {
    View layout;
    private OnProviderSelectedListener listener;
    private List<API.DNSEntry> localEntries = new ArrayList<>();
    private DefaultDNSAdapter adapter;
    private boolean removeButtonShown;
    private RecyclerView list;
    private List<API.DNSEntry> removal = new ArrayList<>();

    public DefaultDNSDialog(@NonNull final Context context, final int theme, @NonNull final OnProviderSelectedListener listener) {
        super(context, theme);
        localEntries = API.loadDNSEntriesFromDatabase(context);
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
                        new DNSCreationDialog(context, new DNSCreationDialog.OnCreationFinishedListener() {
                            @Override
                            public void onCreationFinished(String name, String dns1, String dns2, String dns1V6, String dns2V6) {
                                saveEntryToDatabase(new API.DNSEntry(0,name, dns1, dns2, dns1V6, dns2V6, ""));
                                localEntries.clear();
                                localEntries = API.loadDNSEntriesFromDatabase(context);
                                list.setAdapter(adapter = new DefaultDNSAdapter());
                            }
                        }).show();
                    }
                });
                getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                getButton(BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeButtonShown = false;
                        v.setVisibility(View.INVISIBLE);
                        for(API.DNSEntry entry: removal){
                            API.removeDNSEntry(entry.getID());
                            localEntries.remove(entry);
                        }
                        removal.clear();
                        list.setAdapter(adapter = new DefaultDNSAdapter());
                    }
                });
            }
        });
        setButton(BUTTON_NEUTRAL, context.getString(R.string.remove), (OnClickListener)null);
    }

    private API.DNSEntry saveEntryToDatabase(API.DNSEntry entry){
        SQLiteDatabase database = API.getDatabase(getContext());
        ContentValues values = new ContentValues(5);
        values.put("Name", entry.getName());
        values.put("dns1", entry.getDns1());
        values.put("dns2", entry.getDns2());
        values.put("dns1v6", entry.getDns1V6());
        values.put("dns2v6", entry.getDns2V6());
        values.put("description", entry.getDescription());
        database.insert("DNSEntries", null,values);
        return entry;
    }



    public static interface OnProviderSelectedListener{
        public void onProviderSelected(String name, String dns1, String dns2, String dns1V6, String dns2V6);
    }

    private class DefaultDNSAdapter extends RecyclerView.Adapter<DefaultDNSAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder{
            private View layout;

            public ViewHolder(View itemView) {
                super(itemView);
                this.layout = itemView;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.item_default_dns, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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
                        removeButtonShown = true;
                    }
                    if(!v.isSelected())removal.remove((API.DNSEntry)v.getTag());
                    else removal.add((API.DNSEntry) v.getTag());
                    if(removal.size() == 0){
                        removeButtonShown = false;
                        getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
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
                        if(!v.isSelected())removal.remove((API.DNSEntry)v.getTag());
                        else removal.add((API.DNSEntry) v.getTag());
                        if(removal.size() == 0){
                            removeButtonShown = false;
                            getButton(BUTTON_NEUTRAL).setVisibility(View.INVISIBLE);
                        }
                    }else{
                        dismiss();
                        API.DNSEntry entry = (API.DNSEntry)v.getTag();
                        listener.onProviderSelected(entry.getName(), entry.getDns1(), entry.getDns2(), entry.getDns1V6(), entry.getDns2V6());
                    }
                }
            });
            if(localEntries.get(position).getDescription().equals(""))holder.layout.findViewById(R.id.text2).setVisibility(View.GONE);
            else ((TextView)holder.layout.findViewById(R.id.text2)).setText(localEntries.get(position).getDescription());
            holder.layout.setTag(localEntries.get(position));
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
