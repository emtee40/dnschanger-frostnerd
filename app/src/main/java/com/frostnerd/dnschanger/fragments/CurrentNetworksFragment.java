package com.frostnerd.dnschanger.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.general.Utils;

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
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CurrentNetworksFragment extends Fragment {
    private List<DNSProperties> dnsProperties = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.fragment_current_networks, container, false);
        ConnectivityManager mgr = Utils.requireNonNull((ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE));
        DNSProperties dnsProperty;
        boolean vpnRunning = Util.isServiceThreadRunning();
        for(Network ntw: mgr.getAllNetworks()){
            dnsProperty = new DNSProperties(mgr.getLinkProperties(ntw));
            if(dnsProperty.ipv4Servers.size() == 0 && dnsProperty.ipv6Servers.size() == 0)continue;
            if(!vpnRunning || !dnsProperty.networkName.equals("tun0"))dnsProperties.add(dnsProperty);
        }
        final ListView list = content.findViewById(R.id.list);
        final ArrayAdapter<DNSProperties> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dnsProperties);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int index, long l) {
                StringBuilder serverText = new StringBuilder();
                final DNSProperties properties = dnsProperties.get(index);
                boolean port = PreferencesAccessor.areCustomPortsEnabled(requireContext());
                for(IPPortPair ipPortPair: properties.ipv4Servers){
                    serverText.append(ipPortPair.toString(port)).append("\n");
                }
                for(IPPortPair ipPortPair: properties.ipv6Servers){
                    serverText.append(ipPortPair.toString(port)).append("\n");
                }
                String text = getString(R.string.text_dns_configuration).replace("[name]", properties.networkName);
                text = text.replace("[servers]", serverText);
                new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext())).
                        setMessage(text).setTitle(R.string.dialog_title_dns_configuration).setCancelable(true).
                        setPositiveButton(R.string.use_these_servers, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(requireContext()),
                                        ipv6Enabled = PreferencesAccessor.isIPv6Enabled(requireContext());
                                if(ipv4Enabled && properties.ipv4Servers.size() == 0){
                                    new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext()))
                                    .setMessage(requireContext().getString(R.string.take_dns_configuration_missing_type).replace("[type]", "IPv4"))
                                    .setCancelable(true).setTitle(R.string.warning).setNeutralButton(R.string.close, null)
                                    .setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            PreferencesAccessor.setIPv6Enabled(requireContext(), true);
                                            PreferencesAccessor.setIPv4Enabled(requireContext(), false);
                                            setDNSServersOf(properties);
                                        }
                                    }).setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setDNSServersOf(properties);
                                        }
                                    }).show();
                                }else if(ipv6Enabled && properties.ipv6Servers.size() == 0) {
                                    new AlertDialog.Builder(requireContext(), ThemeHandler.getDialogTheme(requireContext()))
                                            .setMessage(requireContext().getString(R.string.take_dns_configuration_missing_type).replace("[type]", "IPv6"))
                                            .setCancelable(true).setTitle(R.string.warning).setNeutralButton(R.string.close, null)
                                            .setPositiveButton(R.string.disable, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    PreferencesAccessor.setIPv4Enabled(requireContext(), true);
                                                    PreferencesAccessor.setIPv6Enabled(requireContext(), false);
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
        boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(requireContext()),
                ipv6Enabled = PreferencesAccessor.isIPv6Enabled(requireContext());
        if(ipv6Enabled && properties.ipv6Servers.size() != 0){
            PreferencesAccessor.Type.DNS1_V6.saveDNSPair(requireContext(), properties.ipv6Servers.get(0));
            if(properties.ipv6Servers.size() >= 2){
                PreferencesAccessor.Type.DNS2_V6.saveDNSPair(requireContext(), properties.ipv6Servers.get(1));
            }else PreferencesAccessor.Type.DNS2_V6.saveDNSPair(requireContext(), IPPortPair.getEmptyPair());
        }else if(ipv6Enabled) PreferencesAccessor.Type.DNS2_V6.saveDNSPair(requireContext(), IPPortPair.getEmptyPair());

        if(ipv4Enabled && properties.ipv4Servers.size() != 0){
            PreferencesAccessor.Type.DNS1.saveDNSPair(requireContext(), properties.ipv4Servers.get(0));
            if(properties.ipv4Servers.size() >= 2){
                PreferencesAccessor.Type.DNS2.saveDNSPair(requireContext(), properties.ipv4Servers.get(1));
            }else PreferencesAccessor.Type.DNS2.saveDNSPair(requireContext(), IPPortPair.getEmptyPair());
        }else if(ipv4Enabled) PreferencesAccessor.Type.DNS2.saveDNSPair(requireContext(), IPPortPair.getEmptyPair());
        if(Util.isServiceRunning(requireContext()))
            requireContext().startService(DNSVpnService.getUpdateServersIntent(requireContext(), true, false));
        Toast.makeText(requireContext(), R.string.dns_configuration_taken, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(DNSProperties properties: dnsProperties) properties.destroy();
        dnsProperties.clear();
        dnsProperties = null;
    }

    private static class DNSProperties{
        private String networkName;
        private List<IPPortPair> ipv4Servers = new ArrayList<>(),
                ipv6Servers = new ArrayList<>();

        DNSProperties(LinkProperties properties){
            if(TextUtils.isEmpty(properties.getInterfaceName()))networkName = "unknown";
            else networkName = properties.getInterfaceName();

            for(InetAddress address: properties.getDnsServers()){
                if(address == null) continue;
                if(address instanceof Inet6Address) ipv6Servers.add(IPPortPair.wrap(address.getHostAddress(), 53));
                else if(address instanceof Inet4Address)ipv4Servers.add(IPPortPair.wrap(address.getHostAddress(), 53));
            }
        }

        @Override
        public String toString() {
            return networkName;
        }

        private void destroy(){
            ipv4Servers.clear();
            ipv6Servers.clear();
            ipv4Servers = null;
            ipv6Servers = null;
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            destroy();
        }
    }
}
