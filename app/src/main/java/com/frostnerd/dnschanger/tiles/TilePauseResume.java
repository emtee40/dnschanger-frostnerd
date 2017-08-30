package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.API.API;
import com.frostnerd.dnschanger.API.VPNServiceArgument;
import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
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
public class TilePauseResume extends android.service.quicksettings.TileService {
    private static final String LOG_TAG = "[PauseResumeTile]";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        LogFactory.writeMessage(this, LOG_TAG, "Tile added");
        Tile tile = getQsTile();
        if(tile != null){
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel(getString(R.string.not_running));
            tile.updateTile();
        }else API.updateTiles(this);
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
            LogFactory.writeMessage(this, LOG_TAG, "Service running");
            tile.setState(Tile.STATE_ACTIVE);
            if(API.isServiceThreadRunning()){
                LogFactory.writeMessage(this, LOG_TAG, "Service-Thread running");
                tile.setLabel(getString(R.string.tile_pause));
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_pause));
            }else{
                LogFactory.writeMessage(this, LOG_TAG, "Service-Thread not running");
                tile.setLabel(getString(R.string.tile_resume));
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_resume));
            }
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Service not running (State set to unavailable)");
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setLabel(getString(R.string.not_running));
        }
        tile.updateTile();
        LogFactory.writeMessage(this, LOG_TAG, "Tile updated");
    }

    @Override
    public void onClick() {
        super.onClick();
        LogFactory.writeMessage(this, LOG_TAG, "Tile clicked");
        if(!API.isServiceRunning(this)){
            LogFactory.writeMessage(this, LOG_TAG, "Service not running. Returning");
            return;
        }
        boolean pinProtected = Preferences.getBoolean(this, "pin_tile", false),
                running = API.isServiceThreadRunning();
        Intent i;
        if(pinProtected){
            LogFactory.writeMessage(this, LOG_TAG, "Tile is Pin protected. Starting PinActivity",
                    i = new Intent(this, PinActivity.class).putExtra(running ? VPNServiceArgument.COMMAND_STOP_VPN.toString() :
                            VPNServiceArgument.COMMAND_START_VPN.toString(), true).putExtra("redirectToService",true));
            startActivity(i);
        }else{
            LogFactory.writeMessage(this, LOG_TAG, "Tile is not Pin protected. Starting DNSVPNService",
                    i = running ? DNSVpnService.getStopVPNIntent(this) : DNSVpnService.getStartVPNIntent(this));
            startService(i);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LogFactory.writeMessage(this, LOG_TAG, "Stop listening");
    }
}
