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
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.utility.ItemUtil

abstract class Menu(val player: Player) : InventoryHolder {
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

    fun open() {
        runTaskIfNotPrimaryThread {
            if (setbackCondition())
                setbackMenu()?.open()
            else
                // 1 tick delay as online player list updates can be late
                runTaskLater({ player.openInventory(getInventory()) }, 1)
        }
        GuiHandler.setMenu(player, this)
    }

    fun close() {
        runTaskIfNotPrimaryThread { player.closeInventory() }
        GuiHandler.removeMenu(player)
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
    var page: Int,
    var pages: Int,
    val previousMenu: Menu
) : Menu(player) {
    fun addPaginationItems(inventory: Inventory) {
        val bottomRow = inventory.getSize() - 5

        addItem(inventory, bottomRow, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)

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