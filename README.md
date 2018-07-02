# AntiVPN
Get the best; save money on overpriced plugins and block VPN users!

# Installation
### Single / Personal servers
Simply drop the jar into your "plugins" folder. The auto-generated config should default to reasonable values for you, but you may modify it if you wish.
### Multiple servers / Large networks
Drop the jar into the plugins folder and configure the "sql" section to use MySQL instead of SQLite. RabbitMQ and/or Redis are optional but highly recommended if you have multiple servers.

# Config
https://github.com/egg82/AntiVPN/blob/master/src/main/resources/config.yml

# Commands
/avpnreload - Reloads the plugin configuration. This will disconnect and reconnect (if appropriate) any services configured in the config.yml file.

# Permissions
avpn.admin - allows access to the /avpnreload command
avpn.bypass - players with this node bypass the filter entirely

# Legal
According to the [GDPR](https://eugdprcompliant.com/), [you must specify that you are storing IP information to your players in a privacy policy](https://news.ycombinator.com/item?id=16479995) when using this plugin (actually you need that if you're running a vanilla server without this plugin because of server logs). Depending on how data provided from this API is used, [you may be required to manually remove some data](https://ec.europa.eu/info/law/law-topic/data-protection/reform/rules-business-and-organisations/dealing-citizens/do-we-always-have-delete-personal-data-if-person-asks_en) from the databases.
__Disclaimer__: I am a plugin developer, not a lawyer. This information is provided as a "best guess" and is not legal advice.

# API / Developer Usage
### Maven
    <repository>
      <id>egg82-ninja</id>
      <url>https://www.myget.org/F/egg82-java/maven/</url>
    </repository>

### Latest Repo
https://www.myget.org/feed/egg82-java/package/maven/ninja.egg82.plugins/AntiVPN

### API usage
    VPNAPI.getInstance();
    ...
    boolean isVPN(String ip);

### Example - detect if a player is using a VPN (this example is redundant as the plugin does this already)
    VPNAPI api = VPNAPI.getInstance();
    if (api.isVPN(playerIp)) {
        // Kick the player
    }