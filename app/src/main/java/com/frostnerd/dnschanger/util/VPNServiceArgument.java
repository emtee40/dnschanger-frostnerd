package com.frostnerd.dnschanger.util;

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
public enum VPNServiceArgument {
    COMMAND_START_VPN("start_vpn"), COMMAND_STOP_VPN("stop_vpn"), COMMAND_STOP_SERVICE("destroy_vpn"),
    COMMAND_RESTART_VPN("restart_vpn"),
    FLAG_FIXED_DNS("fixeddns"), FLAG_STARTED_WITH_TASKER("startedWithTasker"), FLAG_GET_BINDER("i_wanna_bind"),
    ARGUMENT_UPSTREAM_SERVERS("upstream_servers"), ARGUMENT_STOP_REASON("reason_for_stop"),
    ARGUMENT_CALLER_TRACE("caller_trace"), FLAG_DONT_START_IF_RUNNING("dont_start_running"), FLAG_DONT_UPDATE_DNS("dont_update_dns"),
    FLAG_DONT_START_IF_NOT_RUNNING("dont_start_not_running");

    private final String argument;
    VPNServiceArgument(String argument){
        this.argument = argument;
    }

    @Override
    public String toString() {
        return argument;
    }

    public String getArgument(){
        return argument;
    }
}
