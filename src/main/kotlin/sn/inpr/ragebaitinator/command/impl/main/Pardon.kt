package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "pardon",
    description = "Disables all persistent trolls for a player.",
    aliases = ["stop"],
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false)
    ]
)
class Pardon : BaseCommand() {
    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val targets = targetPlayers(sender, args.getOrNull(1) ?: return false)
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                var pardoned = 0
                for (command in CommandManager.toggleTrolls)
                    if (command.removePlayer(target, "pardoned")) pardoned++
                val placeholders = placeholdersOr("success", "%prefix% %c1%%amount% %c2%persistent troll(s) disabled for %c1%%player%")
                    .put("amount", pardoned.toString())
                    .put("player", target.name)
                send(sender, target, placeholders)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }
}