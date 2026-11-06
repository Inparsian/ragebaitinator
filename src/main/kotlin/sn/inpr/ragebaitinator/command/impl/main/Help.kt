package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.configuration
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.BaseTroll
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
            type = CommandSyntax.Type.STRING,
            purpose = "Category",
            // these are 'all' and the keys of CommandInfo.Category
            completions = ["all", "explosives", "hazards", "world", "movement", "inventory", "combat", "survival", "chat", "illusions", "misc"],
            description = "The category of trolls to list, or all commands"
        ),
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
        val allowed = CommandManager.commands.filter {
            sender.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}")
        }

        val first = args.getOrNull(1)
        val legacyPage = first?.toIntOrNull()
        when {
            first == null -> overview(sender, allowed)
            // this used to take nothing but a page
            legacyPage != null -> listAll(sender, allowed, legacyPage)
            first.equals("all", true) -> listAll(sender, allowed, args.getOrNull(2)?.toIntOrNull() ?: 1)
            else -> {
                val category = CommandInfo.Category.entries.firstOrNull { it.key.equals(first, true) }
                if (category == null) {
                    val placeholders = placeholdersOr("unknownCategory", "%prefix% &cUnknown category. Use %c1%/rb help %c3%to see them all")
                    send(sender, placeholders)
                } else {
                    listCategory(sender, category, allowed, args.getOrNull(2)?.toIntOrNull() ?: 1)
                }
            }
        }

        return true
    }

    private fun item(command: BaseCommand): String = Placeholders("commands.help.item", listOf(
        "%c3%- %c1%/rb %command% %syntax%%c3%: %c2%%description%"
    ))
        .put("command", command.info.name)
        .put("syntax", command.stringifySyntax())
        .put("description", command.description)
        .toString()

    private fun trollsOf(category: CommandInfo.Category, allowed: List<BaseCommand>): List<BaseCommand>
        = allowed.filter { it is BaseTroll && it.info.category == category }.sortedBy { it.info.name }

    // everything that isn't a troll, and then the categories to find the trolls in
    private fun overview(sender: CommandSender, allowed: List<BaseCommand>) {
        val general = allowed.filter { it !is BaseTroll }
        val categories = CommandInfo.Category.entries
            .map { it to trollsOf(it, allowed).size }
            .filter { [_, count] -> count > 0 }

        val categoryItems = categories.map { [category, count] ->
            Placeholders("commands.help.categoryItem", listOf(
                "%c3%- %c1%/rb help %key% %c3%(%c2%%count% trolls%c3%): %c2%%description%"
            ))
                .put("key", category.key)
                .put("count", count.toString())
                .put("description", category.localDescription)
                .toString()
        }

        if (general.isEmpty() && categoryItems.isEmpty()) {
            send(sender, Placeholders("commands.help.no_help", "%prefix% &cNo commands are available"))
            return
        }

        val placeholders = Placeholders("commands.help.overview", listOf(
            "%c3%Available commands:",
            "%general%",
            "%c3%Troll categories:",
            "%categories%"
        ))
            .put("general", general.joinToString("\n") { item(it) })
            .put("categories", categoryItems.joinToString("\n"))

        send(sender, placeholders)
    }

    private fun listAll(sender: CommandSender, allowed: List<BaseCommand>, page: Int) {
        list(sender, "commands.help.message", listOf("%c3%Available commands:", "%c3%Page %c1%%page% %c3%of %c1%%pages%", "%items%"), allowed, page, null)
    }

    private fun listCategory(sender: CommandSender, category: CommandInfo.Category, allowed: List<BaseCommand>, page: Int) {
        list(
            sender, "commands.help.category",
            listOf("%c3%%category% trolls:", "%c3%Page %c1%%page% %c3%of %c1%%pages%", "%items%"),
            trollsOf(category, allowed), page, category.localName
        )
    }

    private fun list(sender: CommandSender, key: String, default: List<String>, commands: List<BaseCommand>, page: Int, category: String?) {
        if (commands.isEmpty()) {
            send(sender, Placeholders("commands.help.no_help", "%prefix% &cNo commands are available"))
            return
        }

        val pages = MathUtil.pages(commands.size, pageSize)
        val current = page.coerceIn(1, pages)
        val items = commands.chunked(pageSize)[current - 1].map { item(it) }

        val placeholders = Placeholders(key, default)
            .put("category", category ?: "")
            .put("items", items.joinToString("\n"))
            .put("page", current.toString())
            .put("pages", pages.toString())
        send(sender, placeholders)
    }
}
