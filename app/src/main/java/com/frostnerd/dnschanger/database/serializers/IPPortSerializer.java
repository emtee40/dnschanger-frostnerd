package com.frostnerd.dnschanger.database.serializers;

import com.frostnerd.database.orm.Serializer;
import com.frostnerd.dnschanger.database.entities.IPPortPair;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * Code may NOT be used without proper permission, neither in binary nor in source form.
 * All redistributions of this software in source code must retain this copyright header
 * All redistributions of this software in binary form must visibly inform users about usage of this software
 * <p>
 * development@frostnerd.com
 */
public class IPPortSerializer extends Serializer<IPPortPair> {
    @Override
    protected String serializeValue(IPPortPair ipPortPair) {
        return ipPortPair.toString();
    }

    @Override
    public IPPortPair deserialize(String text) {
        return IPPortPair.wrap(text);
    }

    @Override
    public String serializeNull() {
        return "";
    }
}
