package com.frostnerd.dnschanger.database.serializers;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.utils.database.orm.Serializer;

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
    public String serialize(IPPortPair ipPortPair) {
        return ipPortPair.toString();
    }

    @Override
    public IPPortPair deserialize(String text) {
        return IPPortPair.wrap(text);
    }
}
