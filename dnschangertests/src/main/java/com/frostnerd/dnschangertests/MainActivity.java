package com.frostnerd.dnschangertests;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = VpnService.prepare(this);
        if(i != null){
            startActivityForResult(i, 1);
        }else startService(new Intent(this, TestVPNService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent i = VpnService.prepare(this);
        if(i != null){
            startActivityForResult(i, 1);
        }else startService(new Intent(this, TestVPNService.class));
    }
}
