package com.frostnerd.dnschanger.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CurrentNetworksFragment extends Fragment {
    private List<DNSProperties> dnsProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.fragment_current_networks, container, false);
        ConnectivityManager mgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        DNSProperties dnsProperty;
        boolean vpnRunning = Util.isServiceThreadRunning();
        for(Network ntw: mgr.getAllNetworks()){
            dnsProperty = new DNSProperties(mgr.getLinkProperties(ntw));
            if(!vpnRunning || !dnsProperty.networkName.equals("tun0"))dnsProperties.add(dnsProperty);
        }
        final ListView list = content.findViewById(R.id.list);
        final ArrayAdapter<DNSProperties> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dnsProperties);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int index, long l) {
                StringBuilder serverText = new StringBuilder();
                final DNSProperties properties = dnsProperties.get(index);
                boolean port = PreferencesAccessor.areCustomPortsEnabled(getContext());
                for(IPPortPair ipPortPair: properties.ipv4Servers){
                    serverText.append(ipPortPair.toString(port)).append("\n");
                }
                for(IPPortPair ipPortPair: properties.ipv6Servers){
                    serverText.append(ipPortPair.toString(port)).append("\n");
                }
                String text = getString(R.string.text_dns_configuration).replace("[name]", properties.networkName);
                text = text.replace("[servers]", serverText);
                new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext())).
                        setMessage(text).setTitle(R.string.dialog_title_dns_configuration).setCancelable(true).
                        setPositiveButton(R.string.use_these_servers, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(getContext()),
                                        ipv6Enabled = PreferencesAccessor.isIPv6Enabled(getContext());
                                if(ipv4Enabled && properties.ipv4Servers.size() == 0){
                                    new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext()))
                                    .setMessage(getContext().getString(R.string.take_dns_configuration_missing_type).replace("[type]", "IPv4"))
                                    .setCancelable(true).setTitle(R.string.warning).setNeutralButton(R.string.close, null)
                                    .setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            PreferencesAccessor.setIPv4Enabled(getContext(), false);
                                            setDNSServersOf(properties);
                                        }
                                    }).setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setDNSServersOf(properties);
                                        }
                                    }).show();
                                }else if(ipv6Enabled && properties.ipv6Servers.size() == 0) {
                                    new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext()))
                                            .setMessage(getContext().getString(R.string.take_dns_configuration_missing_type).replace("[type]", "IPv6"))
                                            .setCancelable(true).setTitle(R.string.warning).setNeutralButton(R.string.close, null)
                                            .setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    PreferencesAccessor.setIPv6Enabled(getContext(), false);
                                                    setDNSServersOf(properties);
                                                }
                                            }).setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setDNSServersOf(properties);
                                        }
                                    }).show();
                                }else{
                                    setDNSServersOf(properties);
                                }
                            }
                        }).setNeutralButton(R.string.close, null).show();
            }
        });
        return content;
    }


    private void setDNSServersOf(DNSProperties properties){
        boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(getContext()),
                ipv6Enabled = PreferencesAccessor.isIPv6Enabled(getContext());
        if(ipv6Enabled && properties.ipv6Servers.size() != 0){
            if(properties.ipv6Servers.size() >= 1){
                PreferencesAccessor.Type.DNS1_V6.saveDNSPair(getContext(), properties.ipv6Servers.get(0));
            }
            if(properties.ipv6Servers.size() >= 2){
                PreferencesAccessor.Type.DNS2_V6.saveDNSPair(getContext(), properties.ipv6Servers.get(1));
            }else PreferencesAccessor.Type.DNS2_V6.saveDNSPair(getContext(), IPPortPair.getEmptyPair());
        }else if(ipv6Enabled) PreferencesAccessor.Type.DNS2_V6.saveDNSPair(getContext(), IPPortPair.getEmptyPair());

        if(ipv4Enabled && properties.ipv4Servers.size() != 0){
            if(properties.ipv4Servers.size() >= 1){
                PreferencesAccessor.Type.DNS1.saveDNSPair(getContext(), properties.ipv4Servers.get(0));
            }
            if(properties.ipv4Servers.size() >= 2){
                PreferencesAccessor.Type.DNS2.saveDNSPair(getContext(), properties.ipv4Servers.get(1));
            }else PreferencesAccessor.Type.DNS2.saveDNSPair(getContext(), IPPortPair.getEmptyPair());
        }else if(ipv4Enabled) PreferencesAccessor.Type.DNS2.saveDNSPair(getContext(), IPPortPair.getEmptyPair());
        if(Util.isServiceRunning(getContext()))
            getContext().startService(DNSVpnService.getUpdateServersIntent(getContext(), true, false));
        Toast.makeText(getContext(), R.string.dns_configuration_taken, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Context getContext() {
        Context context = super.getContext();
        return context == null ? MainActivity.currentContext : context;
    }

    private class DNSProperties{
        private String networkName;
        private List<IPPortPair> ipv4Servers = new ArrayList<>(),
                ipv6Servers = new ArrayList<>();

        DNSProperties(LinkProperties properties){
            if(TextUtils.isEmpty(properties.getInterfaceName()))networkName = "unknown";
            else networkName = properties.getInterfaceName();

            for(InetAddress address: properties.getDnsServers()){
                if(address instanceof Inet6Address) ipv6Servers.add(IPPortPair.wrap(address.getHostAddress(), 53));
                else if(address instanceof Inet4Address)ipv4Servers.add(IPPortPair.wrap(address.getHostAddress(), 53));
            }
        }

        @Override
        public String toString() {
            return networkName;
        }
    }
}
