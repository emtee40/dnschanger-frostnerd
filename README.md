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

If you want to use parts just write me a simple email. Any of these restrictions might be lifted then. I don't bite ;)
For projects which generate no revenue (direct or indirect) and are generally available up to 10 people or less terms 1 and 5 do not apply. Under these circumstances, if you want shoot me an email about what you want to do with the source code but proper permission is not required. Attribution (term 4) is still required, but only in source code


Have a look at the wiki for this app: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/wikis/home<br>
This app could possibly spy on using the VPN connection which has to be used to apply the chosen DNS Servers. It doesn't. This project is open source so that people familiar with java/Android can check on this promise.
If you are not familiar with it: The VPN is only local. Using the VPN you are assigned an IP-Address which can't be used in the internet (192.168.0.1) and connect to the device only (127.0.0.1:8087),
NOT to an endpoint in the internet. The relevant code lines are those: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/app/src/main/java/com/frostnerd/dnschanger/DNSVpnService.java#L239-245
<br><br>

Feel free to contribute to this project, it's completely free to sign up and I'd be happy to fix issues or implement requests.<br><br>

This project uses my AndroidUtils library. It can be accessed at https://git.frostnerd.com/AndroidApps/AndroidUtils when signed in.

© Daniel Wolf 2018
All rights reserved.<br>
