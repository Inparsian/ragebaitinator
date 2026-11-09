@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.Tab
import sn.inpr.ragebaitinator.gui.TrollToggleSubscriber
import sn.inpr.ragebaitinator.gui.def.PaginationMenu
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.gui.pageOf
import sn.inpr.ragebaitinator.listeners.PlayerJoinSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil

private const val PAGE_SIZE = 36
private const val PARDON_ALL = "pardonAll"

class OverviewMenu(player: Player) : PaginationMenu(
    player,
    page = 1,
    pages = 1,
    previousMenu = MainMenu(player)
), TrollToggleSubscriber, PlayerJoinSubscriber, PlayerQuitSubscriber {
    // the ones with the most on come first
    private fun trolled() = Bukkit.getOnlinePlayers()
        .map { it to activeTrolls(it) }
        .filter { it.second.isNotEmpty() }
        .sortedWith(compareByDescending<Pair<Player, List<*>>> { it.second.size }.thenBy { it.first.name.lowercase() })

    override fun title() = GuiText.text("gui.overview.title", "page" to page.toString(), "pages" to pages.toString())

    override fun getInventory(): Inventory {
        val trolled = trolled()
        pages = maxOf(1, MathUtil.pages(trolled.size, PAGE_SIZE))
        page = page.coerceIn(1, pages)

        val inventory = Bukkit.createInventory(this, 54, title())

        addItem(
            inventory, 4, Material.REDSTONE_LAMP, GuiText.text("gui.overview.summary.name"),
            GuiText.lines(
                "gui.overview.summary.lore",
                "players" to trolled.size.toString(),
                "trolls" to trolled.sumOf { it.second.size }.toString()
            ),
            trolled.isNotEmpty()
        )

        if (trolled.isNotEmpty() && player.hasPermission("ragebaitinator.commands.pardon"))
            addItem(inventory, 8, Material.FEATHER, GuiText.text("gui.overview.pardonAll.name"), GuiText.lines("gui.overview.pardonAll.lore"), false, PARDON_ALL)

        pageOf(trolled, page, PAGE_SIZE).forEachIndexed { index, (trolledPlayer, _) ->
            val head = playerHeadItem(trolledPlayer, hints = false)
            val meta = head.itemMeta!!
            meta.lore = (meta.lore ?: listOf()) + GuiText.lines("gui.overview.head.manage")
            head.itemMeta = meta
            addItem(inventory, 9 + index, head)
        }

        if (trolled.isEmpty())
            addItem(inventory, 22, Material.PAPER, GuiText.text("gui.overview.empty.name"), GuiText.lines("gui.overview.empty.lore"), false)

        addPaginationItems(inventory)
        frame(inventory)
        return inventory
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val action = actionOf(event.currentItem) ?: return
        when {
            action.startsWith(PLAYER) -> Bukkit.getPlayerExact(action.removePrefix(PLAYER))?.let {
                if (event.isRightClick || event.isShiftClick)
                    TargetMenu(player, Selection(it)).open()
                else
                    TrollBrowser(player, Selection(it), Tab.Active).open()
            }
            action == PARDON_ALL -> {
                val names = trolled().map { it.first.name }
                if (names.isNotEmpty())
                    player.performCommand("ragebaitinator pardon ${names.joinToString(",")}")
            }
        }
    }

    override fun onPlayerJoin(event: PlayerJoinEvent) = updateLater()
    override fun onPlayerQuit(event: PlayerQuitEvent) = updateLater()
}
