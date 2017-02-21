package com.frostnerd.dnschanger;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.frostnerd.utils.design.MaterialEditText;
import com.frostnerd.utils.general.Utils;
import com.frostnerd.utils.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button startStopButton;
    private boolean vpnRunning;
    private MaterialEditText met_dns1, met_dns2;
    private EditText dns1, dns2;
    private static final HashMap<String, List<String>> defaultDNS = new HashMap<>();
    private static final List<String> defaultDNSKeys;

    private TextView connectionText;
    private ImageView connectionImage;
    private LinearLayout defaultDNSView;
    private Button rate, info;
    private ImageButton importButton;

    private AlertDialog defaultDnsDialog;

    private LinearLayout wrapper;

    static {
        defaultDNS.put("Google DNS", Arrays.asList("8.8.8.8", "8.8.4.4"));
        defaultDNS.put("OpenDNS", Arrays.asList("208.67.222.222", "208.67.220.220"));
        defaultDNS.put("Level3", Arrays.asList("209.244.0.3", "209.244.0.4"));
        defaultDNS.put("FreeDNS", Arrays.asList("37.235.1.174", "37.235.1.177"));
        defaultDNS.put("Yandex DNS", Arrays.asList("77.88.8.8", "77.88.8.1"));
        defaultDNS.put("Verisign", Arrays.asList("64.6.64.6", "64.6.65.6"));
        defaultDNS.put("Alternate DNS", Arrays.asList("198.101.242.72", "23.253.163.53"));
        defaultDNSKeys = new ArrayList<>(defaultDNS.keySet());
    }

    private void setIndicatorState(boolean vpnRunning) {
        ObjectAnimator colorFade = ObjectAnimator.ofObject(wrapper, "backgroundColor", new ArgbEvaluator(),
                vpnRunning ? Color.parseColor("#2196F3") : Color.parseColor("#4CAF50"),
                vpnRunning ? Color.parseColor("#4CAF50") : Color.parseColor("#2196F3"));
        colorFade.setDuration(400);
        colorFade.start();
        if (vpnRunning) {
            int color = Color.parseColor("#43A047");
            connectionText.setText(R.string.connected);
            connectionImage.setImageResource(R.drawable.ic_thumb_up);
            startStopButton.setBackgroundColor(color);
            met_dns1.setCardColor(color);
            met_dns1.setCardStrokeColor(color);
            met_dns2.setCardColor(color);
            met_dns2.setCardStrokeColor(color);
            defaultDNSView.setBackgroundColor(color);
            rate.setBackgroundColor(color);
            info.setBackgroundColor(color);
            importButton.setBackgroundColor(color);
            startStopButton.setText(R.string.stop);
        } else {
            int color = Color.parseColor("#42A5F5");
            connectionText.setText(R.string.not_connected);
            connectionImage.setImageResource(R.drawable.ic_thumb_down);
            startStopButton.setBackgroundColor(color);
            met_dns1.setCardColor(color);
            met_dns1.setCardStrokeColor(color);
            met_dns2.setCardColor(color);
            met_dns2.setCardStrokeColor(color);
            defaultDNSView.setBackgroundColor(color);
            rate.setBackgroundColor(color);
            info.setBackgroundColor(color);
            importButton.setBackgroundColor(color);
            startStopButton.setText(R.string.start);
        }
    }

    public void rateApp(View v) {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public void openDNSInfoDialog(View v) {
        new AlertDialog.Builder(this).setTitle(R.string.info_dns_button).setMessage(R.string.dns_info_text).setCancelable(true).setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        met_dns1 = (MaterialEditText) findViewById(R.id.met_dns1);
        met_dns2 = (MaterialEditText) findViewById(R.id.met_dns2);
        dns1 = (EditText) findViewById(R.id.dns1);
        dns2 = (EditText) findViewById(R.id.dns2);
        connectionImage = (ImageView)findViewById(R.id.connection_status_image);
        connectionText = (TextView)findViewById(R.id.connection_status_text);
        defaultDNSView = (LinearLayout)findViewById(R.id.default_dns_view);
        rate = (Button)findViewById(R.id.rate);
        info = (Button)findViewById(R.id.dnsInfo);
        wrapper = (LinearLayout)findViewById(R.id.activity_main);
        importButton = (ImageButton)findViewById(R.id.default_dns_view_image);
        dns1.setText(Preferences.getString(MainActivity.this, "dns1", "8.8.8.8"));
        dns2.setText(Preferences.getString(MainActivity.this, "dns2", "8.8.4.4"));
        startStopButton = (Button) findViewById(R.id.startStopButton);
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent i = VpnService.prepare(MainActivity.this);
                if (i != null){
                    new AlertDialog.Builder(MainActivity.this).setTitle(R.string.information).setMessage(R.string.vpn_explain)
                            .setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            startActivityForResult(i, 0);
                        }
                    }).show();
                }
                else onActivityResult(0, RESULT_OK, null);
            }
        });
        dns1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(vpnRunning)stopVpn();
                if (!Utils.isIP(s.toString())) {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    met_dns1.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    Preferences.put(MainActivity.this, "dns1", s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        dns2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(vpnRunning)stopVpn();
                if (!Utils.isIP(s.toString())) {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.INCORRECT);
                } else {
                    met_dns2.setIndicatorState(MaterialEditText.IndicatorState.UNDEFINED);
                    Preferences.put(MainActivity.this, "dns2", s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setIndicatorState(API.checkVPNServiceRunning(this));
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        View layout = getLayoutInflater().inflate(R.layout.dialog_default_dns, null, false);
        final ListView list = (ListView) layout.findViewById(R.id.defaultDnsDialogList);
        list.setAdapter(new DefaultDNSAdapter());
        list.setDividerHeight(0);
        defaultDnsDialog = new AlertDialog.Builder(this).setView(layout).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).setTitle(R.string.default_dns_title).create();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                defaultDnsDialog.cancel();
                List<String> ips = defaultDNS.get(defaultDNSKeys.get(position));
                dns1.setText(ips.get(0));
                dns2.setText(ips.get(1));
            }
        });
    }

    public void openDefaultDNSDialog(View v) {
        defaultDnsDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (!vpnRunning){
                startVpn();
                setIndicatorState(true);
            }else{
                stopVpn();
                setIndicatorState(false);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startVpn() {
        startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn", true));
        vpnRunning = true;
        setIndicatorState(true);
    }

    private void stopVpn() {
        startService(new Intent(this, DNSVpnService.class).putExtra("stop_vpn", true));
        stopService(new Intent(this, DNSVpnService.class));
        vpnRunning = false;
        setIndicatorState(false);
    }

    private class DefaultDNSAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return defaultDNS.size();
        }

        @Override
        public Object getItem(int position) {
            return defaultDNS.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = getLayoutInflater().inflate(R.layout.item_default_dns, parent, false);
            ((TextView) v.findViewById(R.id.text)).setText(defaultDNSKeys.get(position));
            v.setTag(getItem(position));
            return v;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return super.onCreateOptionsMenu(menu);
    }
}
