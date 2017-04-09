package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.utils.preferences.Preferences;

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
public class TileStop extends android.service.quicksettings.TileService {

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
            tile.setState(Tile.STATE_INACTIVE);
        }else{
            tile.setState(Tile.STATE_UNAVAILABLE);
        }
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        if(!API.checkVPNServiceRunning(this))return;
        boolean pinProtected = Preferences.getBoolean(this, "pin_tile", false);
        if(pinProtected){
            startActivity(new Intent(this, PinActivity.class).putExtra("destroy", true).putExtra("redirectToService",true));
        }else{
            startService(new Intent(this, DNSVpnService.class).putExtra("destroy", true));
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }
}
