package com.frostnerd.dnschanger.fragments;

import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.ProgressBar;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.adapters.QueryResultAdapter;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_dnsquery, container, false);
        metQuery = contentView.findViewById(R.id.met_query);
        edQuery = contentView.findViewById(R.id.query);
        runQuery = contentView.findViewById(R.id.run_query);
        resultList = contentView.findViewById(R.id.result_list);
        progress = contentView.findViewById(R.id.progress);

        edQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
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
                runQuery(edQuery.getText().toString() + ".");
            }
        });
        resultList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        return contentView;
    }

    private void runQuery(String queryText){
        progress.setVisibility(View.VISIBLE);
        final String adjustedQuery = queryText.endsWith(".") ? queryText : queryText + ".";
        new Thread(){
            @Override
            public void run() {
                try {
                    Resolver resolver = new SimpleResolver("8.8.8.8");
                    resolver.setTCP(true);
                    Name name = Name.fromString(adjustedQuery);
                    Record record = Record.newRecord(name, Type.ANY, DClass.IN);
                    Message query = Message.newQuery(record);
                    Message response = resolver.send(query);
                    RRset[] answer = response.getSectionRRsets(1),
                            authority = response.getSectionRRsets(2),
                            additional = response.getSectionRRsets(3);
                    final QueryResultAdapter adapter = new QueryResultAdapter(getActivity(), answer, authority, additional);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultList.setAdapter(adapter);
                            progress.setVisibility(View.INVISIBLE);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private boolean isResolvable(String s){
        return NetworkUtil.isDomain(s) || (s != null && !s.equals("") && !s.contains("."));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
