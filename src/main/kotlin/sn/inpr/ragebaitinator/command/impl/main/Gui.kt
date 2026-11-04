package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.gui.impl.MainMenu
import sn.inpr.ragebaitinator.gui.impl.PlayerMenu
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "gui",
    description = "Opens the GUI.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER)
    ]
)
class Gui : BaseCommand() {
    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender is Player) {
            val targets = if (args.size > 1) targetPlayers(sender, args[1]) else listOf()
            val menu = if (targets.isNotEmpty()) PlayerMenu(sender, targets[0]) else MainMenu(sender)
            menu.open()
        } else {
            send(sender, Placeholders(language.mustBeAPlayer))
        }

        return true
    }
}