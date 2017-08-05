package com.frostnerd.dnschanger.API;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 *
 * Terms on usage of my code can be found here: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/README.md
 *
 * <p>
 * development@frostnerd.com
 */
public enum VPNServiceArgument {
    COMMAND_START_VPN("start_vpn"), COMMAND_STOP_VPN("stop_vpn"), COMMAND_STOP_SERVICE("destroy_vpn"), FLAG_FIXED_DNS("fixeddns"),
    FLAG_STARTED_WITH_TASKER("startedWithTasker"), ARGUMENT_STOP_REASON("reason_for_stop"), FLAG_GET_BINDER("i_wanna_bind"),
    ARGUMENT_DNS1("dns1"), ARGUMENT_DNS2("dns2"), ARGUMENT_DNS1V6("dns1-v6"), ARGUMENT_DNS2V6("dns2-v6"),
    ARGUMENT_CALLER_TRACE("caller_trace"), FLAG_DONT_START_IF_RUNNING("dont_start_running"), FLAG_DONT_UPDATE_DNS("dont_update_dns");

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
