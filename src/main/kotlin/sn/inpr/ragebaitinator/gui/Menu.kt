@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.Main.Companion.runTaskIfNotPrimaryThread
import sn.inpr.ragebaitinator.Main.Companion.runTaskTimerAsync
import sn.inpr.ragebaitinator.utility.ItemUtil

abstract class Menu(val player: Player, val live: Boolean) : InventoryHolder {
    private class LiveRunnable(val player: Player, val menu: Menu) : Runnable {
        override fun run() {
            // if at any point there is one item that doesn't reflect the current inventory, update the inventory
            val inventory = menu.getInventory()
            val playerInventory = player.openInventory.topInventory

            if (playerInventory.holder == inventory.holder) {
                if (menu.setbackCondition())
                    menu.setbackMenu()?.open() ?: player.closeInventory()

                for (i in 0 until inventory.size) {
                    val item = inventory.getItem(i)
                    val playerItem = playerInventory.getItem(i)
                    if (playerItem != null && (item == null || item != playerItem)) {
                        menu.open()
                        break
                    }
                }
            } else GuiHandler.removeTask(player)?.cancel()
        }
    }

    fun open() {
        runTaskIfNotPrimaryThread { player.openInventory(getInventory()) }
        if (live) {
            GuiHandler.removeTask(player)?.cancel()
            GuiHandler.addTask(player, runTaskTimerAsync(LiveRunnable(player, this), 0, 1))
        }
    }

    fun close() {
        if (live) GuiHandler.removeTask(player)?.cancel()
        runTaskIfNotPrimaryThread { player.closeInventory() }
    }

    open fun setbackMenu(): Menu? = null
    open fun setbackCondition(): Boolean = false

    open fun onClick(event: InventoryClickEvent) {}

    fun addItem(inventory: Inventory, slot: Int, material: Material, name: String, lore: List<String>, glowing: Boolean) {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        meta.setLore(lore)
        if (glowing) ItemUtil.makeGlow(meta)

        item.itemMeta = meta
        inventory.setItem(slot, item)
    }

    fun addItem(inventory: Inventory, slot: Int, item: ItemStack) {
        inventory.setItem(slot, item)
    }
}

abstract class PaginationMenu(
    player: Player,
    live: Boolean,
    var page: Int,
    var pages: Int,
    val previousMenu: Menu
) : Menu(player, live) {
    fun addPaginationItems(inventory: Inventory, page: Int, pages: Int) {
        val bottomRow = inventory.getSize() - 5

        addItem(inventory, bottomRow, Material.REDSTONE_BLOCK, "§cBack", listOf(), false)
        if (!live) addItem(inventory, bottomRow + 4, Material.STRING, "§dRefresh", listOf(), false)

        if (pages > 1) {
            if (page > 1) {
                addItem(inventory, bottomRow - 3, Material.SPECTRAL_ARROW, "§aFirst Page", listOf(), false)
                addItem(inventory, bottomRow - 2, Material.ARROW, "§aPrevious Page", listOf(), false)
            }

            if (page < pages) {
                addItem(inventory, bottomRow + 2, Material.SPECTRAL_ARROW, "§aNext Page", listOf(), false)
                addItem(inventory, bottomRow + 3, Material.ARROW, "§aLast Page", listOf(), false)
            }
        }
    }

    fun setPage(newPage: Int, predicate: () -> Boolean = { true }) {
        if (predicate()) {
            page = newPage
            open()
        }
    }

    fun nextPage() = setPage(page + 1) { page < pages }
    fun previousPage() = setPage(page - 1) { page > 1 }
    fun firstPage() = setPage(1) { page > 1 }
    fun lastPage() = setPage(pages) { page < pages }

    final override fun onClick(event: InventoryClickEvent) {
        when (event.currentItem?.itemMeta?.displayName) {
            "§cBack" -> previousMenu.open()
            "§dRefresh" -> open()
            "§aFirst Page" -> firstPage()
            "§aPrevious Page" -> previousPage()
            "§aNext Page" -> nextPage()
            "§aLast Page" -> lastPage()
            else -> onClickItem(event)
        }
    }

    open fun onClickItem(event: InventoryClickEvent) {}
}