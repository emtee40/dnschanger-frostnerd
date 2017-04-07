package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.DNSVpnService;
import com.frostnerd.dnschanger.PinActivity;
import com.frostnerd.utils.preferences.Preferences;

import java.util.Random;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
@TargetApi(Build.VERSION_CODES.N)
public class TileStart extends android.service.quicksettings.TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Tile tile = getQsTile();
        if(API.checkVPNServiceRunning(this)){
            tile.setState(Tile.STATE_ACTIVE);
        }else{
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if(API.checkVPNServiceRunning(this))return;
        boolean pinProtected = Preferences.getBoolean(this, "pin_tile", false);
        if(pinProtected){
            startActivity(new Intent(this, PinActivity.class).putExtra("start_vpn", true).putExtra("redirectToService",true));
        }else{
            startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn", true));
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }
}
