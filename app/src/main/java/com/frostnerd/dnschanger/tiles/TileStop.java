package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.API.API;
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
public class TileStop extends android.service.quicksettings.TileService {
    private static final String LOG_TAG = "[StopTile]";

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
        if(API.isServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service running (Tile set to inactive)");
            tile.setState(Tile.STATE_INACTIVE);
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Service not running (Tile set to unavailable)");
            tile.setState(Tile.STATE_UNAVAILABLE);
        }
        tile.updateTile();
        LogFactory.writeMessage(this, LOG_TAG, "Tile updated");
    }

    @Override
    public void onClick() {
        super.onClick();
        LogFactory.writeMessage(this, LOG_TAG, "Tile clicked");
        if(!API.isServiceRunning(this))return;
        boolean pinProtected = Preferences.getBoolean(this, "pin_tile", false);
        Intent i;
        if(pinProtected){
            LogFactory.writeMessage(this, LOG_TAG, "Tile is Pin Protected. Starting PinActivity",
                    i=new Intent(this, PinActivity.class).putExtra("destroy", true).putExtra("redirectToService",true));
            startActivity(i);
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Tile is not Pin Protected. Destroying DNSVPNService",
                    i=DNSVpnService.getDestroyIntent(this));
            startService(i);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LogFactory.writeMessage(this, LOG_TAG, "Stop listening");
    }
}
