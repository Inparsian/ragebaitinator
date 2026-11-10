package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Targets
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "info",
    description = "Shows info about a player, like which trolls are active on them.",
)
class Info : BaseCommand() {
    private val target = required<Targets>("")

    override val syntax = listOf(target)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        if (args.input(target) == null)
            return false

        val targets = args.getOrNull(target)
        if (targets == null) {
            send(sender, Placeholders(language.playerNotFound))
            return true
        }

        targets.forEach { send(sender, it, describe(it)) }
        return true
    }

    private fun describe(player: RbPlayer): Placeholders {
        val body = player.bukkit
        val location = body?.location
        val active = PlayerUtil.getEnabledTrolls(player).sorted()
        val immune = CommandManager.trolls.count { player.isImmune(it) }

        val trolls = if (active.isEmpty())
            Placeholders("commands.info.noTrolls", "%c2%None").toString()
        else
            Placeholders(active.joinToString("%c3%, ") { "%c1%$it" })

        return Placeholders("commands.info.message", listOf(
            "%c3%Info on %c1%%player%%c3%:",
            "%c3%- Health: %c1%%health%%c3%, Food: %c1%%food%%c3%, Gamemode: %c1%%gamemode%",
            "%c3%- World: %c1%%world% %c3%at %c1%%x% %y% %z%%c3%, Ping: %c1%%ping%ms",
            "%c3%- Immune to: %c1%%immune% %c3%troll(s)",
            "%c3%- Active trolls (%c1%%count%%c3%): %trolls%"
        ))
            .put("player", player.name)
            .put("health", "%.1f".format(player.health))
            .put("food", player.foodLevel.toString())
            .put("gamemode", player.gameMode.name.lowercase())
            .put("world", location?.world?.name ?: "unknown")
            .put("x", (location?.blockX ?: 0).toString())
            .put("y", (location?.blockY ?: 0).toString())
            .put("z", (location?.blockZ ?: 0).toString())
            .put("ping", (body?.ping ?: 0).toString())
            .put("immune", immune.toString())
            .put("count", active.size.toString())
            .put("trolls", trolls.toString())
    }
}
