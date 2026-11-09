@file:Suppress("UsePropertyAccessSyntax")
package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.gui.CommandHistorySubscriber
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.def.PaginationMenu
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import sn.inpr.ragebaitinator.utility.TimeUtil

private const val PAGE_SIZE = 36

class HistoryMenu(player: Player) : PaginationMenu(
    player,
    page = 1,
    pages = MathUtil.pages(CommandHistory.size, PAGE_SIZE),
    previousMenu = MainMenu(player)
), CommandHistorySubscriber {
    fun historyItem(entry: CommandHistory.CommandHistoryEntry): ItemStack {
        val item = if (entry.playerName == "!!") ItemStack(Material.PAPER) else PlayerUtil.getHead(entry.playerName)
        val meta = item.itemMeta ?: return ItemStack(Material.BARRIER)
        val executor = if (entry.playerName == "!!") GuiText.text("gui.history.server") else entry.playerName
        meta.setDisplayName(GuiText.text("gui.history.entry.name", "command" to entry.command))
        meta.lore = GuiText.lines(
            "gui.history.entry.lore",
            "command" to entry.command, "executor" to executor, "time" to TimeUtil.epochToDate(entry.timestamp)
        )
        item.itemMeta = meta
        return item
    }

    override fun title() = GuiText.text("gui.history.title", "page" to page.toString(), "pages" to pages.toString())

    override fun getInventory(): Inventory {
        pages = maxOf(1, MathUtil.pages(CommandHistory.size, PAGE_SIZE))
        page = page.coerceIn(1, pages)
        val items = CommandHistory.getAllHistory()
            .chunked(PAGE_SIZE).getOrElse(page - 1) { emptyList() }
            .map { historyItem(it) }

        val inv: Inventory = Bukkit.createInventory(this, 45, title())
        inv.contents = items.toTypedArray()
        addPaginationItems(inv)
        frame(inv)
        return inv
    }
}