[![build status](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/badges/master/build.svg)](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/commits/master)
<br>You can download all artifacts at https://dl.frostnerd.com/appbuilds
They are unsigned.<br><br>


This is the source code for my app DNS Changer, which can be found here: https://play.google.com/store/apps/details?id=com.frostnerd.dnschanger<br><br>

Have a look at the wiki for this app: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/wikis/home<br>
This app could possibly spy on using the VPN connection which has to be used to apply the choosen DNS Servers. It doesn't. This project is open source so that people familiar with java/Android can check on this promise.
If you are not familiar with it: The VPN is only local. Using the VPN you are assigned an IP-Address which can't be used in the internet (192.168.0.1) and connect to the device only (127.0.0.1:8087),
NOT to an endpoint in the internet. The relevant code lines are those: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/app/src/main/java/com/frostnerd/dnschanger/DNSVpnService.java#L239-245
<br><br>

Feel free to contribute to this project, it's completely free to sign up and I'd be happy to fix issues or implement requests.<br><br>


© Daniel Wolf 2019

This app uses the library dnsjava. License:

>Copyright (c) 1998-2011, Brian Wellington.
>All rights reserved.
>
>Redistribution and use in source and binary forms, with or without
>modification, are permitted provided that the following conditions are met:
>
>  * Redistributions of source code must retain the above copyright notice,
>    this list of conditions and the following disclaimer.
>
>  * Redistributions in binary form must reproduce the above copyright notice,
>    this list of conditions and the following disclaimer in the documentation
>    and/or other materials provided with the distribution.
>
>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
>AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
>IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
>ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
>LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
>CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
>SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
>INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
>CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
>ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
>POSSIBILITY OF SUCH DAMAGE.