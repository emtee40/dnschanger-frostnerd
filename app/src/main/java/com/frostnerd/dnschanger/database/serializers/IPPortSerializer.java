package com.frostnerd.dnschanger.database.serializers;

import com.frostnerd.dnschanger.database.entities.IPPortPair;
import com.frostnerd.utils.database.orm.Serializer;

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
