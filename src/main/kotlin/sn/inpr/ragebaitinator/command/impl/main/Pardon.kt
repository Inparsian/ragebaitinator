package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "pardon",
    description = "Disables all persistent trolls for a player.",
    aliases = ["stop"],
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER)
    ]
)
class Pardon : BaseCommand() {
    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.size > 1) {
            val targets = targetPlayers(sender, args[1])
            if (targets.isNotEmpty()) {
                targets.forEach { target ->
                    var pardoned = 0
                    for (command in CommandManager.commands.filterIsInstance<BaseToggleTroll>()) {
                        if (command.removePlayer(target, "pardoned")) pardoned++
                    }
                    send(sender, "&c${pardoned} &7persistent troll(s) disabled for &c${target.name}")
                }
            } else {
                send(sender, "&cNo players found.")
            }
        } else {
            send(sender, "&cSpecify a player.")
        }

        return true
    }
}