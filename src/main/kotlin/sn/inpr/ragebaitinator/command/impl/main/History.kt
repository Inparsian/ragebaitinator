package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.runTaskAsync
import sn.inpr.ragebaitinator.SQLite
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
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
    private val pageSize = 10

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        runTaskAsync {
            val history = if (args.size > 2) {
                if (args[2] == "!!") {
                    SQLite.CommandHistory.getConsoleHistory()
                } else {
                    // always prioritize exact if it is in the database
                    SQLite.CommandHistory.getHistory(args[2]).ifEmpty {
                        val targets = targetPlayers(sender, args[2])
                        targets.flatMap { SQLite.CommandHistory.getHistory(it.name) }
                    }
                }
            } else {
                SQLite.CommandHistory.getAllHistory()
            }

            val pages = MathUtil.pages(history.size, pageSize)
            val page = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1..pages) ?: 1
            val paginated = history.chunked(pageSize)[page - 1]

            if (paginated.isNotEmpty()) {
                val items = paginated.map {
                    val date = TimeUtil.epochToDate(it.timestamp)
                    "&7- &4${it.playerName.replace("!!", "&dServer")}: &c/${it.command} &8($date)"
                }

                send(sender, "&7History:")
                send(sender, "&7Page &c$page &7of &c$pages")
                items.forEach { send(sender, it) }
            } else {
                send(sender, "&cNo command history found.")
            }
        }

        return true
    }
}