# AntiVPN
Get the best; save money on overpriced plugins and block VPN users!

# Installation
### Single / Personal servers
Simply drop the jar into your "plugins" folder. The auto-generated config should default to reasonable values for you, but you may modify it if you wish.
### Multiple servers / Large networks
Drop the jar into the plugins folder and configure the "sql" section to use MySQL instead of SQLite. RabbitMQ and/or Redis are optional but highly recommended if you have multiple servers.

# Configs
Bukkit: https://github.com/egg82/AntiVPN/blob/master/Bukkit/src/main/resources/config.yml

Bungee: https://github.com/egg82/AntiVPN/blob/master/Bungee/src/main/resources/config.yml

# Commands
/avpnreload - Reloads the plugin configuration. This will disconnect and reconnect (if appropriate) any services configured in the config.yml file.

/avpntest <ip> - Test an IP through the various (enabled) services. Note that this forces a check so will use credits every time it's run.

/avpncheck <ip> - Check an IP using the default system. This will return exactly the same value as any other API call.

/avpnscore <source> - Scores a particular source based on a pre-made list of known good and bad IPs. Note that this forces a check so will use credits every time it's run.

# Permissions
avpn.admin - allows access to the /avpnreload, /avpntest, /avpncheck, and /avpnscore commands

avpn.bypass - players with this node bypass the filter entirely

# Legal
According to the [GDPR](https://eugdprcompliant.com/), [you must specify that you are storing IP information to your players in a privacy policy](https://news.ycombinator.com/item?id=16479995) when using this plugin (actually you need that if you're running a vanilla server without this plugin because of server logs). Depending on how data provided from this API is used, [you may be required to manually remove some data](https://ec.europa.eu/info/law/law-topic/data-protection/reform/rules-business-and-organisations/dealing-citizens/do-we-always-have-delete-personal-data-if-person-asks_en) from the databases.

__Disclaimer__: I am a plugin developer, not a lawyer. This information is provided as a "best guess" and is not legal advice.

# API / Developer Usage
### Maven
```XML
<repository>
    <id>egg82-ninja</id>
    <url>https://www.myget.org/F/egg82-java/maven/</url>
</repository>
```

### Latest Repo
https://www.myget.org/feed/egg82-java/package/maven/ninja.egg82.plugins/AntiVPN

### API usage
```Java
VPNAPI.getInstance();
...
boolean isVPN(String ip, [boolean expensive]);
ImmutableMap<String, Optional<Boolean>> test(String ip); // WARNING: Does not cache results
double consensus(String ip, [boolean expensive]);
Optional<Boolean> getResult(String ip, String sourceName); // WARNING: Does not cache results
```

### Example - Detect if a player is using a VPN (cascade)
```Java
VPNAPI api = VPNAPI.getInstance();
if (api.isVPN(playerIp)) {
    // Do something
}
```

### Example - Detect if a player is using a VPN (consensus)
```Java
VPNAPI api = VPNAPI.getInstance();
if (api.consensus(playerIp) >= threshold) { // Anywhere from 0.0 to 1.0
    // Do something
}
```

### Example - See which services detect a given IP as a VPN
```Java
VPNAPI api = VPNAPI.getInstance();
ImmutableMap<String, Optional<Boolean>> response = api.test(ip);
for (Entry<String, Optional<Boolean>> kvp : response) {
    // Do something
}
```

### Example - Get the most updated result from a specified provider
```Java
VPNAPI api = VPNAPI.getInstance();
Optional<Boolean> result = api.getResult(playerIp);
if (!result.isPresent()) {
    // Error- ran out of credits, too many attempts, etc etc
    return;
}
if (result.get().booleanValue()) {
    // Do something
} else {
    // Do something else
}
```