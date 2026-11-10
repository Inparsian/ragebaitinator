package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "pardon",
    description = "Disables all persistent trolls for a player.",
    aliases = ["stop"],
)
class Pardon : BaseCommand() {
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
        if (targets != null) {
            targets.forEach { target ->
                var pardoned = 0
                for (command in CommandManager.toggleTrolls)
                    if (command.removePlayer(target, RemoveReason.PARDONED)) pardoned++
                val placeholders = placeholdersOr("success", "%prefix% %c1%%amount% %c2%persistent troll(s) disabled for %c1%%player%")
                    .put("player", target.name)
                    .put("amount", pardoned.toString())
                send(sender, target, placeholders)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }
}