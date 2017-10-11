package com.frostnerd.dnschanger.dialogs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class NewRuleDialog extends AlertDialog{

    public NewRuleDialog(@NonNull Context context, CreationListener listener) {
        super(context);
    }


    public interface CreationListener{
        public void creationFinished(String host, String target, boolean ipv6, boolean wildcard);
    }
}
