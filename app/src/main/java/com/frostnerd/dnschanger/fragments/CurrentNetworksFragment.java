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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.MainActivity;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;

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
    private List<LinkProperties> linkProperties = new ArrayList<>();
    private List<String> networkNames = new ArrayList<>();

    //Done in the constructor because the fragment is created asynchronous and thus no loading indicator has to be shown
    public CurrentNetworksFragment(){
        ConnectivityManager mgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        for(Network ntw: mgr.getAllNetworks()){
            linkProperties.add(mgr.getLinkProperties(ntw));
            String name = linkProperties.get(linkProperties.size() - 1).getInterfaceName();
            networkNames.add(name == null ? "unknown" : name);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View content = inflater.inflate(R.layout.fragment_current_networks, container, false);
        final ListView list = content.findViewById(R.id.list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, networkNames);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int index, long l) {
                StringBuilder serverText = new StringBuilder();
                for(InetAddress address: linkProperties.get(index).getDnsServers()){
                    serverText.append(address.getHostAddress()).append("\n");
                }
                String intName =  networkNames.get(index);
                String text = getString(R.string.text_dns_configuration).replace("[name]", intName);
                text = text.replace("[servers]", serverText);
                new AlertDialog.Builder(getContext(), ThemeHandler.getDialogTheme(getContext())).
                        setMessage(text).setTitle(R.string.dialog_title_dns_configuration).setCancelable(true).
                        setPositiveButton(R.string.use_these_servers, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int v4Index = 0, v6Index = 0;
                                boolean ipv4Enabled = PreferencesAccessor.isIPv4Enabled(getContext()),
                                        ipv6Enabled = PreferencesAccessor.isIPv6Enabled(getContext());
                                for(InetAddress address : linkProperties.get(index).getDnsServers()){
                                    if(address instanceof Inet4Address && ipv4Enabled){
                                        v4Index++;
                                        if(v4Index == 1)PreferencesAccessor.Type.DNS1.saveDNSPair(getContext(),
                                                IPPortPair.wrap(address.getHostAddress(), 53));
                                        else if(v4Index == 2)PreferencesAccessor.Type.DNS2.saveDNSPair(getContext(),
                                                IPPortPair.wrap(address.getHostAddress(), 53));
                                    }else if(address instanceof Inet6Address && ipv6Enabled){
                                        v6Index++;
                                        if(v6Index == 1)PreferencesAccessor.Type.DNS1_V6.saveDNSPair(getContext(),
                                                IPPortPair.wrap(address.getHostAddress(), 53));
                                        else if(v6Index == 2)PreferencesAccessor.Type.DNS2_V6.saveDNSPair(getContext(),
                                                IPPortPair.wrap(address.getHostAddress(), 53));
                                    }
                                    if((v4Index == 2 || !ipv4Enabled) && (v6Index == 2 || !ipv6Enabled))break;
                                }
                            }
                        }).setNeutralButton(R.string.close, null).show();
            }
        });
        return content;
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
}
