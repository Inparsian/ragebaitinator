@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.gui.PaginationMenu
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import sn.inpr.ragebaitinator.utility.TypeUtil

private const val PAGE_SIZE = 36

private fun playerHeadItem(player: Player, inTheirMenu: Boolean): ItemStack {
    val head = PlayerUtil.getHead(player)
    val meta = head.itemMeta!!
    val lore: MutableList<String> = mutableListOf()
    val totalToggleTrolls = CommandManager.toggleTrolls.size
    val enabledToggleTrolls = PlayerUtil.getEnabledTrolls(player).size
    lore.add(String.format("§7Trolls §8(§a%s§8:§c%s§8:§7%s§8)§7:", enabledToggleTrolls, totalToggleTrolls - enabledToggleTrolls, totalToggleTrolls))
    lore.addAll(TypeUtil.splitAtLength(PlayerUtil.getColoredTrollInfo(player).joinToString(", "), 60, ""))
    if (!inTheirMenu) lore.addAll(listOf("", "§8Click to manage this player."))
    meta.lore = lore
    head.itemMeta = meta
    return head
}

class PlayersMenu(player: Player) : PaginationMenu(
    player,
    true,
    1,
    MathUtil.pages(Bukkit.getOnlinePlayers().size, PAGE_SIZE),
    MainMenu(player)
) {
    override fun getInventory(): Inventory {
        val items: MutableList<ItemStack> = mutableListOf()
        val players: List<Player> = Bukkit.getOnlinePlayers().chunked(PAGE_SIZE)[page - 1]
        pages = MathUtil.pages(players.size, PAGE_SIZE)
        for (player in players) {
            val head = playerHeadItem(player, false)
            items.add(head)
        }

        val inv: Inventory = Bukkit.createInventory(this, 45, "Players ($page/$pages)")
        inv.contents = items.toTypedArray()
        addPaginationItems(inv, page, pages)
        return inv
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val target = Bukkit.getPlayer(event.currentItem!!.itemMeta!!.displayName.split("§f")[1])
        if (target != null) PlayerMenu(player, target).open() else open()
    }
}

class PlayerMenu(player: Player, val target: Player) : Menu(player, true) {
    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, target.name)
        val acceptableTrolls = CommandManager.trolls
            .filter { player.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}") }

        addItem(inventory, 10, playerHeadItem(target, true))

        if (player.hasPermission("ragebaitinator.commands.pardon"))
            addItem(inventory, 12, Material.FEATHER, "§9Pardon", listOf("§7Disables all persistent trolls for this player."), false)

        if (acceptableTrolls.isNotEmpty())
            addItem(inventory, 14, Material.BLAZE_POWDER, "§eTroll", listOf("§7Trolls this player."), false)

        addItem(inventory, 16, Material.REDSTONE_BLOCK, "§cBack", listOf(), false)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (event.currentItem?.itemMeta?.displayName) {
            "§cBack" -> PlayersMenu(player).open()
            "§eTroll" -> TrollsMenu(player, target).open()
            "§9Pardon" -> {
                player.performCommand("ragebaitinator pardon ${target.name}")
                open()
            }
        }
    }

    override fun setbackCondition() = !target.isOnline
    override fun setbackMenu() = PlayersMenu(player)
}