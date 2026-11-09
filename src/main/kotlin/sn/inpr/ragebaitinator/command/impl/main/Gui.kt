package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Targets
import sn.inpr.ragebaitinator.gui.impl.MainMenu
import sn.inpr.ragebaitinator.gui.impl.PlayerMenu
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "gui",
    description = "Opens the GUI.",
)
class Gui : BaseCommand() {
    private val target = optional<Targets>("")

    override val syntax = listOf(target)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        if (sender is Player) {
            val target = args[target]?.first()?.bukkit
            val menu = if (target != null) PlayerMenu(sender, target) else MainMenu(sender)
            menu.open()
        } else {
            send(sender, Placeholders(language.mustBeAPlayer))
        }

        return true
    }
}