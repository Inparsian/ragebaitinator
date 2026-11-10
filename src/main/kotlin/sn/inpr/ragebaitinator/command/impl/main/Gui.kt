package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Targets
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.gui.impl.MainMenu
import sn.inpr.ragebaitinator.gui.impl.TargetMenu
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "gui",
    description = "Opens the GUI.",
)
class Gui : BaseCommand() {
    private val targets = optional<Targets>("")

    override val syntax = listOf(targets)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        if (sender is Player) {
            val targets = args[targets]?.map { it.name }.orEmpty()
            val menu = if (targets.isNotEmpty()) TargetMenu(sender, Selection(targets)) else MainMenu(sender)
            menu.open()
        } else {
            send(sender, Placeholders(language.mustBeAPlayer))
        }

        return true
    }
}