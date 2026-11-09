@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.gui.*
import sn.inpr.ragebaitinator.gui.def.PaginationMenu
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.ItemUtil
import sn.inpr.ragebaitinator.utility.MathUtil

private const val TAB = "tab:"
private const val SEARCH = "search"
private const val CLEAR_SEARCH = "clearSearch"
private const val GRID_START = 18
private const val GRID_SIZE = 27

class TrollBrowser(
    player: Player,
    val selection: Selection,
    var tab: Tab = Tab.All,
) : PaginationMenu(
    player,
    page = 1,
    pages = 1,
    previousMenu = TargetMenu(player, selection)
), TrollToggleSubscriber, PlayerQuitSubscriber {
    private fun keyOf(tab: Tab) = when (tab) {
        Tab.Active -> "active"
        Tab.All -> "all"
        is Tab.Category -> "cat:" + tab.key
        is Tab.Search -> SEARCH
    }

    private fun tabOf(key: String): Tab? = when {
        key == "active" -> Tab.Active
        key == "all" -> Tab.All
        key.startsWith("cat:") -> Tab.Category(key.removePrefix("cat:"))
        else -> null
    }

    private fun tabTitle(tab: Tab) = when (tab) {
        Tab.Active -> GuiText.text("gui.browser.tabs.active.title")
        Tab.All -> GuiText.text("gui.browser.tabs.all.title")
        is Tab.Category -> CommandInfo.Category.entries.firstOrNull { it.key == tab.key }?.localName ?: tab.key
        is Tab.Search -> GuiText.text("gui.browser.tabs.search.title", "query" to tab.query)
    }

    private fun isActive(troll: BaseTroll) = enabledOn(troll, selection).isNotEmpty()

    private fun rows(trolls: List<BaseTroll>) = trolls.map { troll ->
        TrollRow(troll.info.name, troll.description, troll.info.category.key, troll.info.category.localName, isActive(troll))
    }

    private fun visibleTrolls(permitted: List<BaseTroll>): List<BaseTroll> {
        val byName = permitted.associateBy { it.info.name }
        return browse(tab, rows(permitted)).mapNotNull { byName[it.name] }
    }

    override fun title() = GuiText.text(
        "gui.browser.title",
        "target" to selection.label, "tab" to tabTitle(tab), "page" to page.toString(), "pages" to pages.toString()
    )

    private fun tabItem(tab: Tab, material: Material, name: String, lore: List<String>): ItemStack {
        val selected = this.tab == tab
        val item = ItemStack(material)
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)
        meta.setDisplayName(GuiText.text(if (selected) "gui.browser.tab.selectedName" else "gui.browser.tab.name", "tab" to name))
        meta.lore = lore + listOf("", GuiText.text(if (selected) "gui.browser.tab.viewing" else "gui.browser.tab.view"))
        if (selected) ItemUtil.makeGlow(meta)
        tag(meta, TAB + keyOf(tab))
        item.itemMeta = meta
        return item
    }

    private fun tabItems(permitted: List<BaseTroll>): List<ItemStack> {
        val items = mutableListOf<ItemStack>()

        val activeCount = permitted.count { isActive(it) }
        items.add(tabItem(
            Tab.Active, Material.REDSTONE_TORCH, GuiText.text("gui.browser.tabs.active.name"),
            GuiText.lines(
                "gui.browser.tabs.active.lore", "count" to activeCount.toString(), "target" to selection.label,
                multiline = mapOf("description" to GuiText.wrap(GuiText.text("gui.browser.tabs.active.description")))
            )
        ))
        items.add(tabItem(
            Tab.All, Material.NETHER_STAR, GuiText.text("gui.browser.tabs.all.name"),
            GuiText.lines(
                "gui.browser.tabs.all.lore", "count" to permitted.size.toString(),
                multiline = mapOf("description" to GuiText.wrap(GuiText.text("gui.browser.tabs.all.description")))
            )
        ))

        for (category in CommandInfo.Category.entries) {
            val trolls = permitted.filter { it.info.category == category }
            if (trolls.isEmpty())
                continue

            val toggles = trolls.filterIsInstance<BaseToggleTroll>()
            items.add(tabItem(
                Tab.Category(category.key), category.icon, category.localName,
                GuiText.lines(
                    "gui.browser.tabs.category.lore",
                    "count" to trolls.size.toString(),
                    "enabled" to toggles.count { isActive(it) }.toString(),
                    "toggles" to toggles.size.toString(),
                    "target" to selection.label,
                    multiline = mapOf("description" to GuiText.wrap(category.localDescription))
                )
            ))
        }

        return items
    }

    private fun trollItem(troll: BaseTroll): ItemStack {
        val total = selection.players.size
        val enabled = enabledOn(troll, selection)
        val reasons = blockedReasons(troll, selection)

        val item = ItemStack(if (reasons.isNotEmpty()) Material.BARRIER else troll.info.icon)
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)
        if (enabled.isNotEmpty()) ItemUtil.makeGlow(meta)
        meta.setDisplayName(trollName(troll, enabled.size, total))
        tag(meta, TROLL + troll.info.name)

        val lore = GuiText.wrap(troll.description).toMutableList()
        lore.add("")
        lore.add(stateLine(troll, enabled.size, total))
        if (reasons.isNotEmpty()) {
            lore.add(GuiText.text("gui.troll.blocked.header"))
            lore.addAll(reasons.take(5))
            if (reasons.size > 5)
                lore.add(GuiText.text("gui.troll.blocked.more", "count" to (reasons.size - 5).toString()))
        }

        lore.add("")
        val word = if (troll is BaseToggleTroll) "toggle" else "execute"
        when {
            !hasOptions(troll) -> lore.add(GuiText.text("gui.troll.click.$word"))
            runsWithDefaults(troll) -> {
                lore.add(GuiText.text("gui.troll.click.$word"))
                lore.add(GuiText.text("gui.troll.click.options"))
            }
            else -> lore.add(GuiText.text("gui.troll.click.configure"))
        }
        if (enabled.isNotEmpty() && enabled.size < total)
            lore.add(GuiText.text("gui.troll.click.partial"))

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    private fun emptyItem(): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta!!
        meta.setDisplayName(GuiText.text("gui.browser.empty.name"))
        meta.lore = when (val tab = tab) {
            Tab.Active -> GuiText.lines("gui.browser.empty.active", "target" to selection.label)
            is Tab.Search -> GuiText.lines("gui.browser.empty.search", "query" to tab.query)
            else -> GuiText.lines("gui.browser.empty.none")
        }
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val permitted = permittedTrolls(player)
        val visible = visibleTrolls(permitted)
        pages = maxOf(1, MathUtil.pages(visible.size, GRID_SIZE))
        page = page.coerceIn(1, pages)

        val inventory = Bukkit.createInventory(this, 54, title())

        // the tabs have two rows, and anything past that would be on top of the trolls
        tabItems(permitted).take(GRID_START).forEachIndexed { slot, item -> addItem(inventory, slot, item) }
        pageOf(visible, page, GRID_SIZE).forEachIndexed { index, troll -> addItem(inventory, GRID_START + index, trollItem(troll)) }
        if (visible.isEmpty())
            addItem(inventory, GRID_START + 13, emptyItem())

        addPaginationItems(inventory)
        addItem(
            inventory, extraSlot(inventory, 1), Material.COMPASS, GuiText.text("gui.browser.search.name"),
            GuiText.lines("gui.browser.search.lore"), tab is Tab.Search, SEARCH
        )
        if (tab is Tab.Search)
            addItem(inventory, extraSlot(inventory, 0), Material.BARRIER, GuiText.text("gui.browser.clear.name"), GuiText.lines("gui.browser.clear.lore"), false, CLEAR_SEARCH)

        frame(inventory)
        return inventory
    }

    private fun showTab(newTab: Tab) {
        tab = newTab
        page = 1
        update()
    }

    private fun search() {
        close()
        GuiHandler.ask(player, ChatInput(this, GuiText.text("gui.browser.search.field")) { text ->
            val query = text.trim()
            tab = if (query.isEmpty()) Tab.All else Tab.Search(query)
            page = 1
            send(player, Placeholders("gui.messages.searchSet", "%prefix% %c2%Searching for %c1%%query%").put("query", query))
        })
    }

    private fun options(troll: BaseTroll) {
        TrollOptionsMenu(player, troll, selection) {
            TrollBrowser(player, selection, tab).also { it.page = page }
        }.open()
    }

    private fun clickTroll(event: InventoryClickEvent, name: String) {
        val troll = CommandManager.getCommand(name) as? BaseTroll ?: return
        when {
            !hasOptions(troll) -> runTroll(player, troll, selection)
            event.isRightClick -> options(troll)
            runsWithDefaults(troll) -> runTroll(player, troll, selection)
            else -> options(troll)
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        val action = actionOf(event.currentItem) ?: return
        when {
            action.startsWith(TAB) -> showTab(tabOf(action.removePrefix(TAB)) ?: return)
            action == SEARCH -> search()
            action == CLEAR_SEARCH -> showTab(Tab.All)
            action.startsWith(TROLL) -> clickTroll(event, action.removePrefix(TROLL))
        }
    }

    override fun setbackCondition() = selection.isGone
    override fun setbackMenu() = PlayersMenu(player)

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player.name in selection.names)
            runTaskLater({ if (selection.isGone) setbackMenu().open() else update() }, 2)
    }
}
