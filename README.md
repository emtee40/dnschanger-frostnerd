[![build status](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/badges/master/build.svg)](https://git.frostnerd.com/PublicAndroidApps/DnsChanger/commits/master)
<br>You can download all artifacts at https://dl.frostnerd.com/appbuilds
They are unsigned.<br><br>


This is the source code for my app DNS Changer, which can be found here: https://play.google.com/store/apps/details?id=com.frostnerd.dnschanger<br><br>

Have a look at the wiki for this app: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/wikis/home<br>
This app could possibly spy on using the VPN connection which has to be used to apply the chosen DNS Servers. It doesn't. This project is open source so that people familiar with java/Android can check on this promise.
If you are not familiar with it: The VPN is only local. Using the VPN you are assigned an IP-Address which can't be used in the internet (192.168.0.1) and connect to the device only (127.0.0.1:8087),
NOT to an endpoint in the internet. The relevant code lines are those: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/app/src/main/java/com/frostnerd/dnschanger/DNSVpnService.java#L239-245
<br><br>

Feel free to contribute to this project, it's completely free to sign up and I'd be happy to fix issues or implement requests.<br><br>

This project uses my AndroidUtils library. It can be accessed at https://git.frostnerd.com/AndroidApps/AndroidUtils when signed in.

© Daniel Wolf 2019
All rights reserved.<br>
