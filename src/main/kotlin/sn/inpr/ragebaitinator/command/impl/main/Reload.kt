package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.configuration
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.TimeUtil

@CommandInfo(
    name = "reload",
    description = "Reloads the configs"
)
class Reload : BaseCommand() {
    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        val reloadTime = TimeUtil.timeTask {
            configuration.reload()
            language.reload()
            CommandManager.commands.forEach(BaseCommand::onReload)
        }

        val placeholders = placeholdersOr("success", "%prefix% %c2%Configs reloaded in %c1%%time%")
            .put("time", TimeUtil.formatNanoInSeconds(reloadTime))
        send(sender, placeholders)

        return true
    }
}