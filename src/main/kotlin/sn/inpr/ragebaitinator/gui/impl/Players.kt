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
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.Tab
import sn.inpr.ragebaitinator.gui.TrollToggleSubscriber
import sn.inpr.ragebaitinator.gui.def.Menu
import sn.inpr.ragebaitinator.gui.def.PaginationMenu
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.gui.pageOf
import sn.inpr.ragebaitinator.listeners.PlayerJoinSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.utility.ItemUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import sn.inpr.ragebaitinator.utility.TypeUtil

private const val PAGE_SIZE = 36
private const val PARDON = "pardon"
private const val BROWSE = "browse"
private const val ACTIVE = "active"
private const val SELECT_ALL = "selectAll"
private const val CLEAR = "clear"
private const val CONTINUE = "continue"

// a tooltip with every troll that is on in it would be taller than the screen
private const val MAX_TROLL_NAMES = 12

const val PLAYER = "player:"

fun activeTrolls(player: Player) = CommandManager.toggleTrolls.filter { it.hasPlayer(player) }

fun playerHeadItem(player: Player, selected: Boolean = false, hints: Boolean = true): ItemStack {
    val head = PlayerUtil.getHead(player)
    val meta = head.itemMeta!!

    val active = activeTrolls(player)
    val names = active.take(MAX_TROLL_NAMES).joinToString(", ") { GuiText.text("gui.troll.name.enabled", "troll" to it.info.name) }
    val trolls = when {
        active.isEmpty() -> listOf(GuiText.text("gui.players.head.none"))
        active.size > MAX_TROLL_NAMES ->
            TypeUtil.splitAtLength(names, 60, "") + GuiText.text("gui.players.head.more", "count" to (active.size - MAX_TROLL_NAMES).toString())
        else -> TypeUtil.splitAtLength(names, 60, "")
    }

    meta.setDisplayName(GuiText.text(if (selected) "gui.players.head.selectedName" else "gui.players.head.name", "player" to player.name))
    val lore = GuiText.lines(
        "gui.players.head.lore",
        "enabled" to active.size.toString(),
        "total" to CommandManager.toggleTrolls.size.toString(),
        multiline = mapOf("trolls" to trolls)
    )
    if (selected) lore.addAll(GuiText.lines("gui.players.head.selected"))
    if (hints) lore.addAll(GuiText.lines("gui.players.head.manage"))
    if (selected) ItemUtil.makeGlow(meta)

    meta.lore = lore
    Menu.tag(meta, PLAYER + player.name)
    head.itemMeta = meta
    return head
}

class PlayersMenu(player: Player, selected: Collection<String> = emptyList()) : PaginationMenu(
    player,
    page = 1,
    pages = 1,
    previousMenu = MainMenu(player)
), TrollToggleSubscriber, PlayerJoinSubscriber, PlayerQuitSubscriber {
    private val selected = LinkedHashSet(selected)

    private fun online() = Bukkit.getOnlinePlayers().sortedBy { it.name.lowercase() }

    override fun title() = GuiText.text("gui.players.title", "page" to page.toString(), "pages" to pages.toString())

    override fun getInventory(): Inventory {
        val online = online()
        // someone who has left can't be chosen any more
        selected.retainAll(online.map { it.name }.toSet())
        pages = maxOf(1, MathUtil.pages(online.size, PAGE_SIZE))
        page = page.coerceIn(1, pages)

        val inventory = Bukkit.createInventory(this, 45, title())
        pageOf(online, page, PAGE_SIZE).forEachIndexed { slot, candidate -> addItem(inventory, slot, playerHeadItem(candidate, candidate.name in selected)) }
        addPaginationItems(inventory)

        if (selected.isEmpty()) {
            addItem(inventory, extraSlot(inventory, 0), Material.HOPPER, GuiText.text("gui.players.selection.all.name"), GuiText.lines("gui.players.selection.all.lore"), false, SELECT_ALL)
        } else {
            addItem(inventory, extraSlot(inventory, 0), Material.BARRIER, GuiText.text("gui.players.selection.clear.name"), GuiText.lines("gui.players.selection.clear.lore"), false, CLEAR)
            addItem(
                inventory, extraSlot(inventory, 1), Material.EMERALD_BLOCK, GuiText.text("gui.players.selection.continue.name"),
                GuiText.lines("gui.players.selection.continue.lore", "count" to selected.size.toString()), true, CONTINUE
            )
        }

        frame(inventory)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val action = actionOf(event.currentItem) ?: return
        when {
            action.startsWith(PLAYER) -> {
                val name = action.removePrefix(PLAYER)
                if (event.isRightClick || event.isShiftClick) {
                    if (!selected.add(name)) selected.remove(name)
                    update()
                } else {
                    Bukkit.getPlayerExact(name)?.let { TargetMenu(player, Selection(it)).open() }
                }
            }
            action == SELECT_ALL -> {
                selected.addAll(Bukkit.getOnlinePlayers().map { it.name })
                update()
            }
            action == CLEAR -> {
                selected.clear()
                update()
            }
            action == CONTINUE -> if (selected.isNotEmpty())
                TargetMenu(player, Selection(selected.toList())).open()
        }
    }

    override fun onPlayerJoin(event: PlayerJoinEvent) = updateLater()
    override fun onPlayerQuit(event: PlayerQuitEvent) = updateLater()
}

class TargetMenu(player: Player, val selection: Selection) : Menu(player), TrollToggleSubscriber, PlayerQuitSubscriber {
    override fun title() = GuiText.text("gui.player.title", "player" to selection.label)

    private fun activeCount() = CommandManager.toggleTrolls.count { troll -> selection.players.any { troll.hasPlayer(it) } }

    private fun headItem(): ItemStack {
        val single = selection.players.singleOrNull()
        if (single != null && !selection.isMany)
            return playerHeadItem(single, hints = false)

        val item = ItemStack(Material.PLAYER_HEAD)
        val meta = item.itemMeta!!
        meta.setDisplayName(GuiText.text("gui.player.group.name", "count" to selection.players.size.toString()))
        meta.lore = GuiText.lines(
            "gui.player.group.lore",
            "enabled" to activeCount().toString(),
            multiline = mapOf("players" to GuiText.wrap(selection.players.joinToString(", ") { it.name }))
        )
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, title())
        addItem(inventory, 10, headItem())

        if (permittedTrolls(player).isNotEmpty()) {
            addItem(inventory, 12, Material.BLAZE_POWDER, GuiText.text("gui.player.browse.name"), GuiText.lines("gui.player.browse.lore"), false, BROWSE)
            val active = activeCount()
            addItem(
                inventory, 13, Material.REDSTONE_TORCH, GuiText.text("gui.player.active.name"),
                GuiText.lines("gui.player.active.lore", "count" to active.toString()), active > 0, ACTIVE
            )
        }

        if (player.hasPermission("ragebaitinator.commands.pardon"))
            addItem(inventory, 14, Material.FEATHER, GuiText.text("gui.player.pardon.name"), GuiText.lines("gui.player.pardon.lore"), false, PARDON)

        addItem(inventory, 16, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)
        frame(inventory)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        when (actionOf(event.currentItem)) {
            BACK -> PlayersMenu(player).open()
            BROWSE -> TrollBrowser(player, selection, Tab.All).open()
            ACTIVE -> TrollBrowser(player, selection, Tab.Active).open()
            PARDON -> if (!selection.isGone) player.performCommand("ragebaitinator pardon ${selection.argument}")
        }
    }

    override fun setbackCondition() = selection.isGone
    override fun setbackMenu() = PlayersMenu(player)

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player.name in selection.names)
            runTaskLater({ if (selection.isGone) setbackMenu().open() else update() }, 2)
    }
}
