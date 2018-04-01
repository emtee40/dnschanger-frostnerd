package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;
import com.frostnerd.utils.lifecyclehelper.UtilityDialog;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
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
}
