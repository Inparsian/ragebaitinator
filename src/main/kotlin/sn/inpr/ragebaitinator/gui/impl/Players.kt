@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.gui.PaginationMenu
import sn.inpr.ragebaitinator.gui.TrollToggleSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerJoinSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import sn.inpr.ragebaitinator.utility.TypeUtil

private const val PAGE_SIZE = 36
private const val PLAYER = "player:"
private const val PARDON = "pardon"
private const val TROLL = "troll"

private fun playerHeadItem(player: Player, inTheirMenu: Boolean): ItemStack {
    val head = PlayerUtil.getHead(player)
    val meta = head.itemMeta!!
    val toggles = CommandManager.toggleTrolls
    val enabled = toggles.count { it.hasPlayer(player) }
    val names = toggles.joinToString(", ") {
        GuiText.text(if (it.hasPlayer(player)) "gui.troll.name.enabled" else "gui.troll.name.disabled", "troll" to it.info.name)
    }

    meta.setDisplayName(GuiText.text("gui.players.head.name", "player" to player.name))
    val lore = GuiText.lines(
        "gui.players.head.lore",
        "enabled" to enabled.toString(),
        "disabled" to (toggles.size - enabled).toString(),
        "total" to toggles.size.toString(),
        multiline = mapOf("trolls" to TypeUtil.splitAtLength(names, 60, ""))
    )
    if (!inTheirMenu) lore.addAll(GuiText.lines("gui.players.head.manage"))
    meta.lore = lore
    Menu.tag(meta, PLAYER + player.name)
    head.itemMeta = meta
    return head
}

class PlayersMenu(player: Player) : PaginationMenu(
    player,
    page = 1,
    pages = MathUtil.pages(Bukkit.getOnlinePlayers().size, PAGE_SIZE),
    previousMenu = MainMenu(player)
), TrollToggleSubscriber, PlayerJoinSubscriber, PlayerQuitSubscriber {
    override fun getInventory(): Inventory {
        val items: MutableList<ItemStack> = mutableListOf()
        val online = Bukkit.getOnlinePlayers().toList()
        pages = MathUtil.pages(online.size, PAGE_SIZE)
        // someone leaving can leave the page that's open past the last one
        page = page.coerceIn(1, maxOf(pages, 1))
        for (player in online.chunked(PAGE_SIZE).getOrElse(page - 1) { emptyList() }) {
            val head = playerHeadItem(player, false)
            items.add(head)
        }

        val inv: Inventory = Bukkit.createInventory(this, 45, GuiText.text("gui.players.title", "page" to page.toString(), "pages" to pages.toString()))
        inv.contents = items.toTypedArray()
        addPaginationItems(inv)
        return inv
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val name = actionOf(event.currentItem)?.takeIf { it.startsWith(PLAYER) }?.removePrefix(PLAYER)
        val target = name?.let { Bukkit.getPlayer(it) }
        if (target != null) PlayerMenu(player, target).open() else open()
    }

    override fun onPlayerJoin(event: PlayerJoinEvent) = open()
    override fun onPlayerQuit(event: PlayerQuitEvent) = open()
}

class PlayerMenu(player: Player, val target: Player) : Menu(player), TrollToggleSubscriber, PlayerQuitSubscriber {
    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, GuiText.text("gui.player.title", "player" to target.name))
        val acceptableTrolls = CommandManager.trolls
            .filter { player.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}") }

        addItem(inventory, 10, playerHeadItem(target, true))

        if (player.hasPermission("ragebaitinator.commands.pardon"))
            addItem(inventory, 12, Material.FEATHER, GuiText.text("gui.player.pardon.name"), GuiText.lines("gui.player.pardon.lore"), false, PARDON)

        if (acceptableTrolls.isNotEmpty())
            addItem(inventory, 14, Material.BLAZE_POWDER, GuiText.text("gui.player.troll.name"), GuiText.lines("gui.player.troll.lore"), false, TROLL)

        addItem(inventory, 16, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (actionOf(event.currentItem)) {
            BACK -> PlayersMenu(player).open()
            TROLL -> TrollCategoriesMenu(player, target).open()
            PARDON -> {
                player.performCommand("ragebaitinator pardon ${target.name}")
                open()
            }
        }
    }

    override fun setbackCondition() = !target.isOnline
    override fun setbackMenu() = PlayersMenu(player)

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player == target) setbackMenu().open()
    }
}
