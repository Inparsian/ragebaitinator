@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.def

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
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.GuiText
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

    private fun openInventory() {
        runTaskIfNotPrimaryThread {
            if (setbackCondition())
                setbackMenu()?.open()
            else
                // 1 tick delay as online player list updates can be late
                runTaskLater({ player.openInventory(getInventory()) }, 1)
        }
    }

    fun update() {
        val view = player.openInventory
        val top = view.topInventory
        if (top.holder == this) {
            top.contents = getInventory().contents
            title()?.let { view.setTitle(it) }
        }
    }

    open fun title(): String? = null

    // someone who has left is still in the list of who is online for a moment,
    // so what shows them has to wait
    fun updateLater() {
        runTaskLater({ update() }, 2)
    }

    fun open() {
        openInventory()
        GuiHandler.setMenu(player, this)
    }

    fun close() {
        runTaskIfNotPrimaryThread { player.closeInventory() }
        GuiHandler.removeMenu(player)
    }

    open fun setbackMenu(): Menu? = null
    open fun setbackCondition(): Boolean = false
    protected open fun onClickPre(event: InventoryClickEvent): Boolean = true
    protected open fun onClick(event: InventoryClickEvent) {}
    fun handleClick(event: InventoryClickEvent) {
        if (onClickPre(event)) onClick(event)
    }

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

    fun frame(inventory: Inventory) {
        val filler = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val meta = filler.itemMeta!!
        meta.setDisplayName(" ")
        ItemUtil.hideTooltips(meta)
        filler.itemMeta = meta
        for (slot in 0 until inventory.getSize())
            if (inventory.getItem(slot) == null)
                inventory.setItem(slot, filler)
    }
}

abstract class PaginationMenu(
    player: Player,
    var page: Int,
    var pages: Int,
    val previousMenu: Menu
) : Menu(player) {
    fun addPaginationItems(inventory: Inventory) {
        val row = inventory.getSize() - 9

        addItem(inventory, row, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)

        if (pages > 1) {
            if (page > 1) {
                addItem(inventory, row + 2, Material.SPECTRAL_ARROW, GuiText.text("gui.common.firstPage.name"), listOf(), false, FIRST_PAGE)
                addItem(inventory, row + 3, Material.ARROW, GuiText.text("gui.common.previousPage.name"), listOf(), false, PREVIOUS_PAGE)
            }

            addItem(
                inventory, row + 4, Material.PAPER,
                GuiText.text("gui.common.page.name", "page" to page.toString(), "pages" to pages.toString()), listOf(), false
            )

            if (page < pages) {
                addItem(inventory, row + 5, Material.ARROW, GuiText.text("gui.common.nextPage.name"), listOf(), false, NEXT_PAGE)
                addItem(inventory, row + 6, Material.SPECTRAL_ARROW, GuiText.text("gui.common.lastPage.name"), listOf(), false, LAST_PAGE)
            }
        }
    }

    fun extraSlot(inventory: Inventory, index: Int): Int = inventory.getSize() - 2 + index

    fun setPage(newPage: Int, predicate: () -> Boolean = { true }) {
        if (predicate()) {
            page = newPage
            update()
        }
    }

    fun nextPage() = setPage(page + 1) { page < pages }
    fun previousPage() = setPage(page - 1) { page > 1 }
    fun firstPage() = setPage(1) { page > 1 }
    fun lastPage() = setPage(pages) { page < pages }

    final override fun onClickPre(event: InventoryClickEvent): Boolean {
        when (actionOf(event.currentItem)) {
            BACK -> previousMenu.open()
            REFRESH -> update()
            FIRST_PAGE -> firstPage()
            PREVIOUS_PAGE -> previousPage()
            NEXT_PAGE -> nextPage()
            LAST_PAGE -> lastPage()
            else -> return true
        }
        return false
    }
}