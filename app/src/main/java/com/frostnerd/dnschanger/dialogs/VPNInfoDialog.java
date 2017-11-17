package com.frostnerd.dnschanger.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;

import com.frostnerd.dnschanger.R;
import com.frostnerd.dnschanger.util.PreferencesAccessor;
import com.frostnerd.dnschanger.util.ThemeHandler;

/**
 * Created by Daniel on 17.11.2017.
 */

public class VPNInfoDialog extends AlertDialog {

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
