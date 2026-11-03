@file:Suppress("UsePropertyAccessSyntax")
package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.SQLite
import sn.inpr.ragebaitinator.gui.PaginationMenu
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import sn.inpr.ragebaitinator.utility.TimeUtil
import kotlin.collections.chunked

private const val PAGE_SIZE = 36

class HistoryMenu(player: Player) : PaginationMenu(
    player,
    false,
    1,
    MathUtil.pages(SQLite.CommandHistory.size, PAGE_SIZE),
    MainMenu(player)
) {
    fun historyItem(entry: SQLite.CommandHistory.CommandHistoryEntry): ItemStack {
        val item = if (entry.playerName == "!!") ItemStack(Material.PAPER) else PlayerUtil.getHead(entry.playerName)
        val meta = item.itemMeta ?: return ItemStack(Material.BARRIER)
        val lore = mutableListOf<String>()
        meta.setDisplayName("§c${entry.command}")
        lore.add("§7Executed by:")
        lore.add("§f" + entry.playerName.replace("!!", "§dServer"))
        lore.add("")
        lore.add("§7Executed at:")
        lore.add("§f" + TimeUtil.epochToDate(entry.timestamp))
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val items = SQLite.CommandHistory.getAllHistory()
            .chunked(PAGE_SIZE)[page - 1]
            .map { historyItem(it) }
        pages = MathUtil.pages(SQLite.CommandHistory.size, PAGE_SIZE)

        val inv: Inventory = Bukkit.createInventory(this, 45, "History ($page/$pages)")
        inv.contents = items.toTypedArray()
        addPaginationItems(inv, page, pages)
        return inv
    }
}