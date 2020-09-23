package com.frostnerd.dnschanger.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.QueryResultAdapter;
import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.dnsquery.Resolver;
import com.frostnerd.dnschanger.util.dnsquery.ResolverResult;
import com.frostnerd.materialedittext.MaterialEditText;
import com.frostnerd.networking.NetworkUtil;

import org.minidns.record.Data;
import org.minidns.record.Record;

import java.io.IOException;
import java.net.SocketTimeoutException;


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
public class DnsQueryFragment extends Fragment {
    private MaterialEditText metQuery;
    private EditText edQuery;
    private Button runQuery;
    private RecyclerView resultList;
    private ProgressBar progress;
    private TextView infoText;
    private CheckBox tcp, any;
    private boolean showingError;
    private QueryResultAdapter adapter;
    private static final String LOG_TAG = "DnsQueryFragment";

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
        any = contentView.findViewById(R.id.query_any);
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
                runQuery(edQuery.getText().toString());
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
        if(resultList != null){
            if(resultList.getAdapter() != null){
                ((QueryResultAdapter)resultList.getAdapter()).destroy();
            }
            resultList.setAdapter(null);
        }
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

    private void runQuery(final String queryText){
        progress.setVisibility(View.VISIBLE);
        final String adjustedQuery = queryText.endsWith(".") ? queryText.substring(0, queryText.length()-1) : queryText;
        new Thread(){
            @Override
            public void run() {
                try {
                    IPPortPair server = getDefaultDNSServer();
                    LogFactory.writeMessage(getContext(), LOG_TAG,"Sending query '" + adjustedQuery + "' to " + server.getAddress() + ":" + server.getPort() + " (tcp: " + tcp.isChecked() + ")");
                    ResolverResult<Data> result = new Resolver(server.getAddress()).resolve(adjustedQuery, any.isChecked() ? Record.TYPE.ANY : Record.TYPE.A, Record.CLASS.IN,
                            tcp.isChecked(), server.getPort());
                    if(!result.wasSuccessful()){
                        throw new IOException(result.getResponseCode().name());
                    }
                    if(isAdded()){
                        if(adapter != null) adapter.destroy();
                        adapter = new QueryResultAdapter(requireContext(), result.getRawAnswer().answerSection);
                        if(isAdded())Util.getActivity(DnsQueryFragment.this).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultList.setAdapter(adapter);
                                progress.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
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

    private void handleException(Exception e){
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
            resultList.setAdapter(null);
            infoText.setText(getString(R.string.query_destination_info).replace("[x]", getDefaultDNSServer().toString(PreferencesAccessor.areCustomPortsEnabled(requireContext()))));
            showingError = false;
        }
    }

    private boolean isResolvable(String s){
        return NetworkUtil.isDomain(s) || !s.equals("") && !s.contains(".");
    }
}
