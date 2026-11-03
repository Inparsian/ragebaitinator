package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "help",
    description = "You are reading it, duh doy",
    syntax = [
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Page",
            description = "The page to display",
            default = "1"
        )
    ]
)
class Help : BaseCommand() {
    val pageSize = 10

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val acceptableList: List<BaseCommand> = CommandManager.commands.filter {
            sender.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}")
        }

        val pages = MathUtil.pages(acceptableList.size, pageSize)
        val page = (args.getOrNull(1)?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val paginated: List<BaseCommand> = acceptableList.chunked(pageSize)[page - 1]

        if (paginated.isNotEmpty()) {
            val items = paginated.map {
                val syntax = it.stringifySyntax()
                "&7- &c/rb ${it.info.name} $syntax&7: &f${it.info.description}"
            }

            send(sender, "&7Available commands:")
            send(sender, "&7Page &c$page &7of &c$pages")
            items.forEach { send(sender, it) }
        } else {
            send(sender, "&cNo help available for this page")
        }

        return true
    }
}