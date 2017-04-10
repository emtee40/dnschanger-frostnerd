package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.API;
import com.frostnerd.dnschanger.LogFactory;
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
public class TileStart extends android.service.quicksettings.TileService {
    private static final String LOG_TAG = "[StartTile]";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        LogFactory.writeMessage(this, LOG_TAG, "Tile added");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        LogFactory.writeMessage(this, LOG_TAG, "Tile removed");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        LogFactory.writeMessage(this, LOG_TAG, "Start listening");
        Tile tile = getQsTile();
        if(API.checkVPNServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service not running (State set to Active)");
            tile.setState(Tile.STATE_ACTIVE);
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Service running (State set to inactive)");
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
        LogFactory.writeMessage(this, LOG_TAG, "Tile updated");
    }

    @Override
    public void onClick() {
        super.onClick();
        LogFactory.writeMessage(this, LOG_TAG, "Tile clicked");
        if(API.checkVPNServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service not running. Returning");
            return;
        }
        boolean pinProtected = Preferences.getBoolean(this, "pin_tile", false);
        if(pinProtected){
            LogFactory.writeMessage(this, LOG_TAG, "Tile is Pin protected. Starting PinActivity");
            startActivity(new Intent(this, PinActivity.class).putExtra("start_vpn", true).putExtra("redirectToService",true));
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Tile is not Pin protected. Starting DNSVPNService");
            startService(new Intent(this, DNSVpnService.class).putExtra("start_vpn", true));
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LogFactory.writeMessage(this, LOG_TAG, "Stop listening");
    }
}
