This is the source code for my app DNS Changer, which can be found here: https://play.google.com/store/apps/details?id=com.frostnerd.dnschanger<br><br>



You may use parts of the source code but are not allowed to copy my source code as a whole or in greater parts. 
Additionally, when using part of my code please inform me AND inform your users that it contains work done by me.<br><br>


Have a look at the wiki for this app: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/wikis/home<br>
This app could possibly spy on using the VPN connection which has to be used to apply the choosen DNS Servers. It doesn't. This project is open source so that people familiar with java/Android can check on this promise.
If you are not familiar with it: The VPN is only local. Using the VPN you are assigned an IP-Address which can't be used in the internet (192.168.0.1) and connect to the device only (127.0.0.1:8087),
NOT to an endpoint in the internet. The relevant code lines are those: https://git.frostnerd.com/PublicAndroidApps/DnsChanger/blob/master/app/src/main/java/com/frostnerd/dnschanger/DNSVpnService.java#L239-245
<br><br>

Feel free to contribute to this project, it's completely free to sign up and I'd be happy to fix issues or implement requests.<br><br>


© Daniel Wolf 2017
All rights reserved.<br>



daniel.wolf@frostnerd.com