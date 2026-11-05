package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.configuration
import sn.inpr.ragebaitinator.Main.Companion.runTaskAsync
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.TimeUtil

@CommandInfo(
    name = "history",
    description = "View command history.",
    syntax = [
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Page",
            description = "The page number.",
            default = "1"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.PLAYER,
            purpose = "Player",
            description = "The player(s) to view the command history of. Leave blank to view all command history. Specify '!!' to view the command history for console."
        )
    ]
)
class History : BaseCommand() {
    private val pageSize: Int get() = configuration.intOr("commands.history.pageSize", 10)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        runTaskAsync {
            val history = if (args.size > 2) {
                if (args[2] == "!!") {
                    CommandHistory.getConsoleHistory()
                } else {
                    // always prioritize exact if it is in the database
                    CommandHistory.getHistory(args[2]).ifEmpty {
                        val targets = targetPlayers(sender, args[2])
                        targets.flatMap { CommandHistory.getHistory(it.name) }
                    }
                }
            } else {
                CommandHistory.getAllHistory()
            }

            val pages = MathUtil.pages(history.size, pageSize)
            val page = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1..pages) ?: 1
            val paginated = history.chunked(pageSize)[page - 1]

            if (paginated.isNotEmpty()) {
                val items = paginated.map {
                    val date = TimeUtil.epochToDate(it.timestamp)
                    val sender = it.playerName.replace("!!", "&dServer")
                    Placeholders("commands.history.item", "%c3%- %c2%%sender%: %c1%/%command% &8(%date%)")
                        .put("sender", sender)
                        .put("command", it.command)
                        .put("date", date)
                        .toString()
                }

                val placeholders = Placeholders("commands.history.message", listOf(
                    "%c3%History:",
                    "%c3%Page %c1%%page% %c3%of %c1%%pages%",
                    "%items%"
                ))
                    .put("items", items.joinToString("\n"))
                    .put("page", page.toString())
                    .put("pages", pages.toString())

                send(sender, placeholders)
            } else {
                send(sender, Placeholders("commands.history.no_history", "%prefix% &cNo command history found"))
            }
        }

        return true
    }
}