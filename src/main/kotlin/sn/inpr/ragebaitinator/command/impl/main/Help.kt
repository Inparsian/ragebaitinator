package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.configuration
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.Placeholders
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
    private val pageSize: Int get() = configuration.intOr("commands.help.pageSize", 10)

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
        val page = (args.getOrNull(1)?.toIntOrNull() ?: 1).coerceIn(1..pages)
        val paginated: List<BaseCommand> = acceptableList.chunked(pageSize)[page - 1]

        if (paginated.isNotEmpty()) {
            val items = paginated.map {
                val syntax = it.stringifySyntax()
                Placeholders("commands.help.item", listOf(
                    "%c3%- %c1%/rb %command% %syntax%%c3%: %c2%%description%"
                ))
                    .put("command", it.info.name)
                    .put("syntax", syntax)
                    .put("description", it.info.description)
                    .toString()
            }

            val placeholders = Placeholders("commands.help.message", listOf(
                "%c3%Available commands:",
                "%c3%Page %c1%%page% %c3%of %c1%%pages%",
                "%items%"
            ))
                .put("items", items.joinToString("\n"))
                .put("page", page.toString())
                .put("pages", pages.toString())

            send(sender, placeholders)
        } else {
            send(sender, Placeholders("commands.help.no_help", "%prefix% &cNo commands are available"))
        }

        return true
    }
}