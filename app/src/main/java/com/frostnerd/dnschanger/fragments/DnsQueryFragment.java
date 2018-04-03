package com.frostnerd.dnschanger.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.QueryResultAdapter;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.networking.NetworkUtil;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public class DnsQueryFragment extends Fragment {
    private MaterialEditText metQuery;
    private EditText edQuery;
    private Button runQuery;
    private RecyclerView resultList;
    private ProgressBar progress;
    private TextView infoText;
    private CheckBox tcp;
    private boolean showingError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_dnsquery, container, false);
        metQuery = contentView.findViewById(R.id.met_query);
        edQuery = contentView.findViewById(R.id.query);
        runQuery = contentView.findViewById(R.id.run_query);
        resultList = contentView.findViewById(R.id.result_list);
        progress = contentView.findViewById(R.id.progress);
        infoText = contentView.findViewById(R.id.query_destination_info_text);
        edQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                resetElements();
                boolean valid = isResolvable(charSequence.toString());
                metQuery.setIndicatorState(valid ? MaterialEditText.IndicatorState.CORRECT : MaterialEditText.IndicatorState.INCORRECT);
                runQuery.setEnabled(valid);
                runQuery.setClickable(valid);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        runQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetElements();
                runQuery(edQuery.getText().toString() + ".");
            }
        });
        tcp = contentView.findViewById(R.id.query_tcp);
        tcp.setChecked(PreferencesAccessor.sendDNSOverTCP(requireContext()));
        resultList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        infoText.setText(getString(R.string.query_destination_info).replace("[x]", getDefaultDNSServer().toString(PreferencesAccessor.areCustomPortsEnabled(requireContext()))));
        return contentView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        metQuery = null;
        edQuery = null;
        runQuery = null;
        resultList = null;
        progress = null;
        infoText = null;
        tcp = null;
    }

    private IPPortPair getDefaultDNSServer(){
        return PreferencesAccessor.isIPv4Enabled(requireContext()) ? PreferencesAccessor.Type.DNS1.getPair(requireContext()) : PreferencesAccessor.Type.DNS1_V6.getPair(requireContext());
    }

    private void runQuery(String queryText){
        progress.setVisibility(View.VISIBLE);
        final String adjustedQuery = queryText.endsWith(".") ? queryText : queryText + ".";
        new Thread(){
            @Override
            public void run() {
                try {
                    IPPortPair server = getDefaultDNSServer();
                    Resolver resolver = new SimpleResolver(server.getAddress());
                    resolver.setPort(server.getPort());
                    resolver.setTCP(tcp.isChecked());
                    Name name = Name.fromString(adjustedQuery);
                    Record record = Record.newRecord(name, Type.ANY, DClass.IN);
                    Message query = Message.newQuery(record);
                    Message response = resolver.send(query);
                    RRset[] answer = response.getSectionRRsets(1),
                            authority = response.getSectionRRsets(2),
                            additional = response.getSectionRRsets(3);
                    if(answer == null)throw new IOException("RESULT NULL");
                    if(isAdded()){
                        final QueryResultAdapter adapter = new QueryResultAdapter(requireContext(), answer, authority, additional);
                        Util.getActivity(DnsQueryFragment.this).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultList.setAdapter(adapter);
                                progress.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                } catch (final IOException e) {
                    if(isAdded())
                        Util.getActivity(DnsQueryFragment.this).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleException(e);
                            }
                        });
                }
            }
        }.start();
    }

    private void handleException(IOException e){
        showingError = true;
        progress.setVisibility(View.INVISIBLE);
        String errorMSG = e.getMessage();
        errorMSG = errorMSG == null ? e.getLocalizedMessage() : errorMSG;
        if(errorMSG == null){
            if(e instanceof SocketTimeoutException)errorMSG = "TIMEOUT";
            else errorMSG = "GENERAL ERROR";
        }
        infoText.setText(getString(R.string.query_error_occured).replace("[error]", errorMSG));
    }

    private void resetElements(){
        if(showingError){
            infoText.setText(getString(R.string.query_destination_info).replace("[x]", getDefaultDNSServer().toString(PreferencesAccessor.areCustomPortsEnabled(requireContext()))));
            showingError = false;
        }
    }

    private boolean isResolvable(String s){
        return NetworkUtil.isDomain(s) || !s.equals("") && !s.contains(".");
    }
}
