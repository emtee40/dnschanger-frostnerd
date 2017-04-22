package com.frostnerd.dnschanger.API;

/**
 * Copyright Daniel Wolf 2017
 * All rights reserved.
 * <p>
 * development@frostnerd.com
 */
public enum VPNServiceArguments {
    COMMAND_START_VPN("start_vpn"), COMMAND_STOP_VPN("stop_vpn"), COMMAND_STOP_SERVICE("destroy"), FLAG_FIXED_DNS("fixeddns"),
    FLAG_STARTED_WITH_TASKER("startedWithTasker"), ARGUMENT_STOP_REASON("reason"), FLAG_GET_BINDER("binder"),
    ARGUMENT_DNS1("dns1"), ARGUMENT_DNS2("dns2"), ARGUMENT_DNS1V6("dns1-v6"), ARGUMENT_DNS2V6("dns2-v6"),
    ARGUMENT_CALLER_TRACE("caller_trace"), FLAG_DONT_START_IF_RUNNING("dont_start_running");

    private final String argument;
    VPNServiceArguments(String argument){
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
