package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;

import java.util.ArrayList;
import java.util.List;


public class DefaultDNSDialog extends AlertDialog {
    View view;
    private static final List<DNSEntry> entries = new ArrayList<>();
    private OnProviderSelectedListener listener;
    static {
        entries.add(new DNSEntry("", "", "", "", ""));
        entries.add(new DNSEntry("Google", "8.8.8.8", "8.8.4.4", "2001:4860:4860::8888", "2001:4860:4860::8844"));
        entries.add(new DNSEntry("OpenDNS", "208.67.222.222", "208.67.220.220", "2620:0:ccc::2", "2620:0:ccd::2"));
        entries.add(new DNSEntry("Level3", "209.244.0.3", "209.244.0.4", "", ""));
        entries.add(new DNSEntry("FreeDNS", "37.235.1.174", "37.235.1.177", "", ""));
        entries.add(new DNSEntry("Yandex", "77.88.8.8", "77.88.8.1", "2a02:6b8::feed:0ff", "2a02:6b8:0:1::feed:0ff"));
        entries.add(new DNSEntry("Verisign", "64.6.64.6", "64.6.65.6", "2620:74:1b::1:1", "2620:74:1c::2:2"));
        entries.add(new DNSEntry("Alternate", "198.101.242.72", "23.253.163.53", "", ""));
        entries.add(new DNSEntry("Norton Connectsafe - Security", "199.85.126.10", "199.85.127.10", "", ""));
        entries.add(new DNSEntry("Norton Connectsafe - Security + Pornography", "199.85.126.20", "199.85.127.20", "", ""));
        entries.add(new DNSEntry("Norton Connectsafe - Security + Portnography + Other", "199.85.126.30", "199.85.127.30", "", ""));
    }
    private List<DNSEntry> localEntries = new ArrayList<>();

    public DefaultDNSDialog(@NonNull Context context,@NonNull final OnProviderSelectedListener listener) {
        super(context);
        for(DNSEntry entry: entries)localEntries.add(entry);
        this.listener = listener;
        view = LayoutInflater.from(context).inflate(R.layout.dialog_default_dns, null, false);
        setView(view);
        final ListView list = (ListView) view.findViewById(R.id.defaultDnsDialogList);
        list.setAdapter(new DefaultDNSAdapter());
        list.setDividerHeight(0);
        setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        setButton(BUTTON_POSITIVE, context.getString(R.string.add), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //TODO
            }
        });
        setTitle(R.string.default_dns_title);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dismiss();
                DNSEntry entry = (DNSEntry)view.getTag();
                listener.onProviderSelected(entry.getName(), entry.dns1, entry.dns2, entry.dns1V6, entry.dns2V6);
            }
        });

    }

    public static interface OnProviderSelectedListener{
        public void onProviderSelected(String name, String dns1, String dns2, String dns1V6, String dns2V6);
    }

    private class DefaultDNSAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return localEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = getLayoutInflater().inflate(R.layout.item_default_dns, parent, false);
            ((TextView) v.findViewById(R.id.text)).setText(localEntries.get(position).getName());
            v.setTag(getItem(position));
            return v;
        }
    }

    private static class DNSEntry implements Comparable<DNSEntry>{
        private String name, dns1,dns2,dns1V6,dns2V6;

        public DNSEntry(String name, String dns1,String dns2, String dns1V6, String dns2V6){
            this.name = name;
            this.dns1 =  dns1;
            this.dns2 = dns2;
            this.dns1V6 = dns1V6;
            this.dns2V6 = dns2V6;
        }

        public String getName() {
            return name;
        }

        public String getDns1() {
            return dns1;
        }

        public String getDns2() {
            return dns2;
        }

        public String getDns1V6() {
            return dns1V6;
        }

        public String getDns2V6() {
            return dns2V6;
        }

        @Override
        public int compareTo(@NonNull DNSEntry o) {
            return name.compareTo(o.name);
        }
    }
}
