# PR Map Logging Discord Bot
Discord bot that logs played maps on Project Reality servers and allows queries on it.

## Running the bot

**Requires Java 17 or greater**

Go to a folder where you want to install the bot and download it
> wget https://github.com/Undermmon/PRMapLoggerBot/releases/download/v24.3.30/maplogger-24.3.30.zip

Then extract the downloaded zip file.
> unzip maplogger-24.3.30.zip

Go to the *bin* folder inside the newly extracted folder using 
> cd maplogger-24.3.30/bin

Edit the configuration file called *config.json*, to do that we are using *nano* a popular terminal file editor.
> nano config.json

The configuration consists of the fallowing:
- realitymod_api -> link to reality mod's rest api that servers ServerInfo.json. You probably don't need to change this.
- token -> You Discord bot token key, **you must replace \<YOUR BOT TOKEN\> with your own**.
- fetch_interval -> How often **in minutes** the bot will retrieve information from  reality mod's api.
- monitored_servers -> Defines which servers should get it's played maps logged, there **must be one or more** monitored servers. The first one is the default, it'll be the one used when no server is specified by users in commands.

\<SERVER NAME\> is the server name to be displayed on the bot commands to users, i.g in autocomplete, etc. It doesn't need to match your server name as it appears ingame.

\<SERVER IDENTIFIER\> is the unique identifier of the server. You can find a server id on ServerInfo.json returned by reality mod's api. 

```JSON
{
	"realitymod_api": "https://servers.realitymod.com/api/ServerInfo",
	"token": "<YOUR BOT TOKEN>",
	"fetch_interval": 5,
	"monitored_servers": {
		"<SERVER NAME>": "<SERVER IDENTIFIER>"
	}
}
```
To save modifications in *nano* press **CTRL+O** then **ENTER**, now exit the editor with **CTRL+X**.

Now run your bot. If you are on *Windows*, you can use *MapLogger.bat*

> ./MapLogger

If you get the error *permission denied* give the file permission to be executed by your user 

> chmod u+x MapLogger

It may take a few seconds to get the bot online. Once started it'll give a link to invite the bot.

> Started sucessfully, you can invite the bot with: ...

If your *config.json* is not properly set you will see this

> [main] ERROR - me.undermon.maplogger.configuration.InvalidConfigurationException: ...

If your bot token is not valid you will see this

> [WritingThread] INFO  - Websocket closed with reason 'Authentication failed.' and code AUTHENTICATION_FAILED (4004) by server!

## Logs and Database

All log messages printed to the console are also appended to a file called *logs* inside the *bin* directory.

Played maps are persisted on a SQlite database called *maps.db* inside the *bin* directory.

Both *logs* and *maps.db* will be automatically created if not found.

## Updating

To update just download and extract the new version, then move your *config.json*, *maps.db*, and optionally *logs*, from the old version directory to the new one.

The project uses Calendar Versioning, so *v24.3.29* means that the update released on the *29th of February, 2024*. In case of multiple updates on the same day, a number is appended to the end, i.e *v24.2.29-1* is the second realease of the day, and so on.

Any compatibility breaking version will be clearly marked so.