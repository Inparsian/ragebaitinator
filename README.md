# Ragebaitinator

A Spigot plugin that allows you to piss off people in block game

- Costs nothing! Completely free and open-source software
- Integration with PlaceholderAPI
- 100+ trolls, many ways to make ppl angry!!!
- Extremely configurable
- Block rollback system that rolls back block changes even after a crash

## So... what can it do?

*A lot.* Ragebaitinator is one of, if not the first troll plugin of its kind to intercept packets using PacketEvents, which allows for more advanced trolls you probably have not seen before.

Here are just some of them:

- Simulate actual client network lag by delaying packet transmission with true FIFO queuing
- Haunt players with a client-side Herobrine
- Harness the power of the Texan atmosphere to summon tornadoes to chase players
- Turn a player into a potato by moving their camera into an item entity
- Make a player fall asleep on the spot with fake sleeping poses, without ever putting them in a bed
- Hide every other player, their tab list entries, and their chat from a player
- Show fake blocks that only the player can see, like a floor that turns to gold, or diamond veins that turn out to be stone
- Give a player a health bar that lies to them
- Rewrite what players say as they say it: UwUspeak, pig latin, MD5 hashes, scrambled words, or silence everyone but themselves

## Building & Running

A compiled .jar for Ragebaitinator is not currently available. However, you can build the plugin from source code.

Ensure you at least have PacketEvents on your server; otherwise Ragebaitinator will not work.

### Building

You will need:

- JDK 25
- Gradle 9 (this repository does not include the Gradle wrapper)
- An internet connection, as dependencies are downloaded from the Spigot, CodeMC, and extendedclip repositories

Clone the repository and run `gradle build`. The plugin will be at `build/libs/Ragebaitinator-<version>.jar`.

If you would like the jar to be built straight into a server's plugins folder, set `deployDir` in `~/.gradle/gradle.properties` (or pass `-PdeployDir=<path>` to Gradle):

```
deployDir=/path/to/server/plugins
```

### Running

You need at least:

- A Spigot server on Minecraft 26.1 or newer (compatibility with server versions lower than 26.1 is W.I.P but has not been tested)
- [PacketEvents](https://github.com/retrooper/packetevents) (built against 2.14.0), which is required as this is not shaded

Put the jar in your `plugins` folder and start the server.

