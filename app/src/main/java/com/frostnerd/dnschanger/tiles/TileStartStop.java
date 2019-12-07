package com.frostnerd.dnschanger.tiles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;

import com.frostnerd.dnschanger.LogFactory;
import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.activities.PinActivity;
import com.frostnerd.dnschanger.services.DNSVpnService;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.Util;
import com.frostnerd.dnschanger.util.VPNServiceArgument;

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
@TargetApi(Build.VERSION_CODES.N)
public class TileStartStop extends android.service.quicksettings.TileService {
    private static final String LOG_TAG = "[StartStopTile]";

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        LogFactory.writeMessage(this, LOG_TAG, "Tile added");
        Tile tile = getQsTile();
        if(tile != null){
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tile_start));
            tile.updateTile();
        }else Util.updateTiles(this);
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
        if(tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            if (Util.isServiceRunning(this)) {
                LogFactory.writeMessage(this, LOG_TAG, "Service not running (State set to Active)");
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel(getString(R.string.tile_stop));
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_stop));
            } else {
                LogFactory.writeMessage(this, LOG_TAG, "Service running (State set to inactive)");
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel(getString(R.string.tile_start));
                tile.setIcon(Icon.createWithResource(this, R.drawable.ic_stat_resume));
            }
            tile.updateTile();
            LogFactory.writeMessage(this, LOG_TAG, "Tile updated");
        }
    }

    @Override
    public void onClick() {
        super.onClick();
        LogFactory.writeMessage(this, LOG_TAG, "Tile clicked");
        boolean pinProtected = PreferencesAccessor.isPinProtected(this, PreferencesAccessor.PinProtectable.TILE),
        running = Util.isServiceRunning(this);
        Intent i;
        if (pinProtected) {
            LogFactory.writeMessage(this, LOG_TAG, "Tile is Pin protected. Starting PinActivity",
                    i = new Intent(this, PinActivity.class).putExtra(running ? VPNServiceArgument.COMMAND_STOP_SERVICE.toString() :
                            VPNServiceArgument.COMMAND_START_VPN.toString(), true).putExtra("redirectToService", true));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } else {
            LogFactory.writeMessage(this, LOG_TAG, "Tile is not Pin protected. Starting DNSVPNService",
                    i = running ? DNSVpnService.getDestroyIntent(this) : DNSVpnService.getStartVPNIntent(this));
            Util.startService(this, i);
        }
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        LogFactory.writeMessage(this, LOG_TAG, "Stop listening");
    }
}
