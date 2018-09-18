[![build status](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/badges/master/build.svg)](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/commits/master)
<br>You can download all artifacts at https://dl.frostnerd.com/appbuilds
They are unsigned.<br><br>


This is the source code for my app DNS Changer, which can be found here: https://play.google.com/store/apps/details?id=com.frostnerd.dnschanger<br><br>


As this project contains many hours of my (unpaid) work and I don't get any revenue from it at all I'm sure most of you understand that I don't want just anybody to come along and reuse my project in large parts, presenting it as their own work. That's why I impose the following restrictions:

1. You may use parts of the source code but are NOT allowed to copy my source code as a whole or in greater parts, unless given proper permission to do so.<br>
2. You are NOT allowed to present parts of my work as your own without proper permission from me.<br>
3. You are NOT allowed to use parts of this commercially without proper permission from me.<br>
4. You MUST give attribution to me when using parts of my code (Visually to possible users and in the Sourcecode).<br>
5. Additionally, when using part of my code please inform me that you want to use parts of my code AND inform your users that it contains work done by me.<br><br>

If you want to use parts just wrint me a simple email. Any of these restrictions might be lifted then. I don't bite ;)
For projects which generate no revenue (direct or indirect) and are generally available up to 10 people or less terms 1 and 5 do not apply. Under these circumstances, if you want shoot me an email about what you want to do with the source code but proper permission is not required. Attribution (term 4) is still required, but only in source code



Have a look at the wiki for this app: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/wikis/home<br>
This app could possibly spy on using the VPN connection which has to be used to apply the choosen DNS Servers. It doesn't. This project is open source so that people familiar with java/Android can check on this promise.
If you are not familiar with it: The VPN is only local. Using the VPN you are assigned an IP-Address which can't be used in the internet (192.168.0.1) and connect to the device only (127.0.0.1:8087),
NOT to an endpoint in the internet. The relevant code lines are those: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/app/src/main/java/com/frostnerd/dnschanger/DNSVpnService.java#L239-245
<br><br>

Feel free to contribute to this project, it's completely free to sign up and I'd be happy to fix issues or implement requests.<br><br>


© Daniel Wolf 2017
All rights reserved.<br>

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