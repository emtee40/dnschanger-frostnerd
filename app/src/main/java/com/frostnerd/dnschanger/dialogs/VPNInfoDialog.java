package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.lifecyclehelper.UtilityDialog;

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
public class VPNInfoDialog extends UtilityDialog {

    public VPNInfoDialog(Context context, final DialogInterface.OnClickListener click){
        super(context, ThemeHandler.getDialogTheme(context));
        if(!PreferencesAccessor.shouldShowVPNInfoDialog(context)){
            click.onClick(this, 0);
        }else{
            View content = getLayoutInflater().inflate(R.layout.dialog_vpn_info, null, false);
            final CheckBox checkBox = content.findViewById(R.id.checkbox);
            setTitle(context.getString(R.string.information) + " - " + context.getString(R.string.app_name));
            setCancelable(false);
            setButton(BUTTON_POSITIVE, context.getString(R.string.ok), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(checkBox.isChecked())PreferencesAccessor.setShowVPNInfoDialog(getContext(), false);
                    click.onClick(dialogInterface, i);
                }
            });
            setView(content);
            show();
        }
    }

    @Override
    protected void destroy() {

    }
}
