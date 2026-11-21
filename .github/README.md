<div align="center">
  <h1><img width="600" alt="image" src="https://github.com/user-attachments/assets/7e6235f1-d8ea-45a9-9369-b4cb78c4abfe" /></h1>
  <img alt="GitHub License" src="https://img.shields.io/github/license/Inparsian/ragebaitinator">
  <img alt="GitHub top language" src="https://img.shields.io/github/languages/top/Inparsian/ragebaitinator">
  <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/Inparsian/ragebaitinator"/>
  <img alt="GitHub commit activity" src="https://img.shields.io/github/commit-activity/w/Inparsian/ragebaitinator"/>
  <img alt="GitHub Repo stars" src="https://img.shields.io/github/stars/Inparsian/ragebaitinator?style=flat"/>
  <img alt="GitHub forks" src="https://img.shields.io/github/forks/Inparsian/ragebaitinator?style=flat"/>
  <img alt="GitHub issues" src="https://img.shields.io/github/issues/Inparsian/ragebaitinator"/>
  <img alt="GitHub pull requests" src="https://img.shields.io/github/issues-pr/Inparsian/ragebaitinator"/>
</div>
<div align="center">
  <h3>
    [<a href="https://www.youtube.com/watch?v=so8fs-452_U">showcase</a>]​&nbsp;
    [<a href="https://discord.gg/qyKzkrfY2t">discord</a>]
  </h3>
</div>
<br>

A Spigot plugin for 26.1+ that allows you to piss off people in block game

- Costs nothing! Completely free and open-source software
- Integration with PlaceholderAPI
- 130+ trolls, many ways to make ppl angry!!!
- Extremely configurable
- Block rollback system that rolls back block changes even after a crash

## ⚠️ Notes

- This is a re-code of my old private Troll plugin (in Java) from 2022. About 90 of the commands and the inventory GUI handler came from it
- This is beta software, please report any bugs or other issues you find :)
- Some AI assistance was used for more tricky portions, but 95% of the plugin was written by hand and AI assisted code has been independently verified
- I can only guarantee support for server versions 26.1 and above. Support for versions below that (down to 1.16, and maybe 1.12 or 1.18) is a work in progress and is far from finished

## ❓ So... what can it do?

*A lot.* Ragebaitinator is one of, if not the first troll plugin of its kind to intercept packets using PacketEvents, which allows for more advanced trolls you probably have not seen before.

Here are just some of them:

- Simulate actual client network lag by delaying packet transmission with true FIFO queuing
- Haunt players with a client-side Herobrine
- Harness the power of the Texan atmosphere to summon tornadoes to chase players
- Turn players into potatoes
- Make players randomly fall asleep
- Chat trolls: UwUspeak, pig latin, scrambled words, pirate, and more

## 📥 Download

The only official download sources for Ragebaitinator are [Spigot](https://www.spigotmc.org/resources/ragebaitinator.140517/) and [Hangar](https://hangar.papermc.io/Inparsian/Ragebaitinator) (and soon Modrinth).

## ⚙️ Building & Running

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

- A Spigot server on Minecraft 26.1 or newer
- [PacketEvents](https://github.com/retrooper/packetevents) (built against 2.14.0), which is required as this is not shaded

Put the jar in your `plugins` folder and start the server.

