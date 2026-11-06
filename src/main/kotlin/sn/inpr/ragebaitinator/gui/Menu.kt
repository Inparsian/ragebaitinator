@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.Main.Companion.runTaskIfNotPrimaryThread
import sn.inpr.ragebaitinator.Main.Companion.runTaskTimerAsync
import sn.inpr.ragebaitinator.utility.ItemUtil

abstract class Menu(val player: Player, val live: Boolean) : InventoryHolder {
    companion object {
        const val BACK = "back"
        const val REFRESH = "refresh"
        const val FIRST_PAGE = "firstPage"
        const val PREVIOUS_PAGE = "previousPage"
        const val NEXT_PAGE = "nextPage"
        const val LAST_PAGE = "lastPage"

        // what an item does when it's clicked is kept on the item itself. what it's called is up to the language file
        private val actionKey get() = Main.makeKey("gui_action")

        fun tag(meta: ItemMeta, action: String) = meta.persistentDataContainer.set(actionKey, PersistentDataType.STRING, action)
        fun actionOf(item: ItemStack?): String? = item?.itemMeta?.persistentDataContainer?.get(actionKey, PersistentDataType.STRING)
    }

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

    fun addItem(inventory: Inventory, slot: Int, material: Material, name: String, lore: List<String>, glowing: Boolean, action: String? = null) {
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        meta.setDisplayName(name)
        meta.setLore(lore)
        if (glowing) ItemUtil.makeGlow(meta)
        if (action != null) tag(meta, action)

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

        addItem(inventory, bottomRow, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)
        if (!live) addItem(inventory, bottomRow + 4, Material.STRING, GuiText.text("gui.common.refresh.name"), listOf(), false, REFRESH)

        if (pages > 1) {
            if (page > 1) {
                addItem(inventory, bottomRow - 3, Material.SPECTRAL_ARROW, GuiText.text("gui.common.firstPage.name"), listOf(), false, FIRST_PAGE)
                addItem(inventory, bottomRow - 2, Material.ARROW, GuiText.text("gui.common.previousPage.name"), listOf(), false, PREVIOUS_PAGE)
            }

            if (page < pages) {
                addItem(inventory, bottomRow + 2, Material.ARROW, GuiText.text("gui.common.nextPage.name"), listOf(), false, NEXT_PAGE)
                addItem(inventory, bottomRow + 3, Material.SPECTRAL_ARROW, GuiText.text("gui.common.lastPage.name"), listOf(), false, LAST_PAGE)
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
        when (actionOf(event.currentItem)) {
            BACK -> previousMenu.open()
            REFRESH -> open()
            FIRST_PAGE -> firstPage()
            PREVIOUS_PAGE -> previousPage()
            NEXT_PAGE -> nextPage()
            LAST_PAGE -> lastPage()
            else -> onClickItem(event)
        }
    }

    open fun onClickItem(event: InventoryClickEvent) {}
}