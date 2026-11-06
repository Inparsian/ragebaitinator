@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.gui.PaginationMenu
import sn.inpr.ragebaitinator.gui.TrollGuiDataInput
import sn.inpr.ragebaitinator.utility.ItemUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.TypeUtil

private const val BACK = "§cBack"
private const val NONE = "§fNone"
private const val PAGE_SIZE = 36

private fun trollsIn(category: CommandInfo.Category?): List<BaseTroll>
    = CommandManager.trolls.filter { category == null || it.info.category == category }.sortedBy { it.info.name }

private fun blocked(troll: BaseTroll, target: Player) = troll.getConditions(target)?.list?.any { it.predicate } == true

private fun runsWithDefaults(command: BaseCommand) = command.info.syntax.drop(1).all { it.optional }

private fun icon(troll: BaseTroll, meta: ItemMeta, enabled: Boolean) {
    ItemUtil.hideTooltips(meta)
    if (troll is BaseToggleTroll) {
        if (enabled) ItemUtil.makeGlow(meta)
        meta.setDisplayName((if (enabled) "§a" else "§c") + troll.info.name)
    } else {
        meta.setDisplayName("§6${troll.info.name}")
    }
}

private fun typeLine(troll: BaseTroll, enabled: Boolean)
    = if (troll is BaseToggleTroll) "§7Type: §bToggle §8(${if (enabled) "§aEnabled" else "§cDisabled"}§8)" else "§7Type: §6One-time"

class TrollCategoriesMenu(player: Player, val target: Player) : Menu(player, true) {
    private companion object {
        val CATEGORY_SLOTS = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24)
        const val BACK_SLOT = 27
        const val ALL_SLOT = 31
        const val ALL_NAME = "§fAll Trolls"
    }

    private fun categoryName(category: CommandInfo.Category) = category.color + category.displayName

    private fun categoryItem(category: CommandInfo.Category): ItemStack? {
        val trolls = CommandManager.trolls.filter { it.info.category == category }
        if (trolls.isEmpty())
            return null

        val toggles = trolls.filterIsInstance<BaseToggleTroll>()
        val enabled = toggles.count { it.hasPlayer(target) }

        val item = ItemStack(category.icon)
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)
        meta.setDisplayName(categoryName(category))
        val lore = TypeUtil.splitAtLength(category.blurb, 50, "§f")
        lore.addAll(listOf(
            "",
            "§7Trolls: §f${trolls.size}",
            "§7Enabled on ${target.name}: §${if (enabled > 0) "a" else "7"}$enabled§8/§7${toggles.size}",
            "",
            "§8Click to view."
        ))
        if (enabled > 0) ItemUtil.makeGlow(meta)
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 36, "Trolls")
        val items = CommandInfo.Category.entries.mapNotNull { categoryItem(it) }
        items.zip(CATEGORY_SLOTS).forEach { [item, slot] -> addItem(inventory, slot, item) }

        addItem(inventory, ALL_SLOT, Material.NETHER_STAR, ALL_NAME, listOf(
            "§7Every troll, in no particular order.",
            "",
            "§7Trolls: §f${CommandManager.trolls.size}",
            "",
            "§8Click to view."
        ), false)
        addItem(inventory, BACK_SLOT, Material.REDSTONE_BLOCK, BACK, listOf(), false)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (val name = event.currentItem?.itemMeta?.displayName) {
            BACK -> PlayerMenu(player, target).open()
            ALL_NAME -> TrollsMenu(player, target, null).open()
            else -> CommandInfo.Category.entries.firstOrNull { categoryName(it) == name }
                ?.let { TrollsMenu(player, target, it).open() }
        }
    }

    override fun setbackCondition() = !target.isOnline()
    override fun setbackMenu() = PlayersMenu(player)
}

class TrollsMenu(player: Player, val target: Player, val category: CommandInfo.Category?) : PaginationMenu(
    player,
    true,
    1,
    MathUtil.pages(trollsIn(category).size, PAGE_SIZE),
    TrollCategoriesMenu(player, target)
) {
    fun addItemCommons(troll: BaseTroll, lore: MutableList<String>, params: List<String>) {
        val word = if (troll is BaseToggleTroll) "toggle" else "execute"
        lore.addAll(listOf("", "§7Conditions:"))

        val conditions = troll.getConditions(target)
        if (conditions != null) {
            for ([humanReadable, _, predicate] in conditions.list) {
                lore.add("§8- §${if (predicate) "c" else "a"}$humanReadable")
            }
        } else {
            lore.add(NONE)
        }

        lore.addAll(listOf("", "§7Additional Parameters:"))
        if (params.isNotEmpty()) lore.addAll(params) else lore.add(NONE)
        lore.addAll(listOf("", "§8Left click to $word."))

        if (runsWithDefaults(troll))
            lore.add("§8Right click to $word with default parameters.")
    }

    fun trollItem(troll: BaseTroll): ItemStack {
        val item = ItemStack(if (blocked(troll, target)) Material.BARRIER else troll.info.icon)
        val meta = item.itemMeta!!
        val lore: MutableList<String> = TypeUtil.splitAtLength(troll.info.description, 50, "§f")
        val params: MutableList<String> = mutableListOf()

        for (syntax in troll.info.syntax.filter { it.purpose.isNotEmpty() }) {
            val type = syntax.type.toString()
            val defaultPart = if (syntax.default.isNotEmpty()) {
                val color = if (syntax.type == CommandSyntax.Type.BOOLEAN) (if (syntax.default.toBoolean()) "§2" else "§4") else "§7"
                " §8(${color}${syntax.default}§8)"
            } else ""
            val requiredPart = if (!syntax.optional) " §c(Required)" else ""

            params.add("§8- §b${syntax.purpose} §7: §f" + type + defaultPart + requiredPart)
        }

        val enabled = troll is BaseToggleTroll && troll.hasPlayer(target)
        icon(troll, meta, enabled)
        lore.addAll(listOf("", typeLine(troll, enabled)))
        addItemCommons(troll, lore, params)

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val items: MutableList<ItemStack> = mutableListOf()
        val trolls = trollsIn(category).chunked(PAGE_SIZE)[page - 1]
        for (command in trolls)
            items.add(trollItem(command))
        val inventory = Bukkit.createInventory(this, 45, "${category?.displayName ?: "All Trolls"} ($page/$pages)")
        inventory.contents = items.toTypedArray()
        addPaginationItems(inventory, page, pages)
        return inventory
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val command = CommandManager.getCommand(event.currentItem?.itemMeta?.displayName?.substring(2))
        if (command != null) {
            if (command.info.syntax.any { it.purpose.isNotEmpty() }) {
                if (event.click == ClickType.LEFT) {
                    TrollMenu(player, command as BaseTroll, target, category).open()
                } else if (event.click == ClickType.RIGHT && runsWithDefaults(command)) {
                    player.performCommand("ragebaitinator ${command.info.name} ${target.name}")
                    open()
                }
            } else {
                player.performCommand("ragebaitinator ${command.info.name} ${target.name}")
                open()
            }
        }
    }

    override fun setbackCondition() = !target.isOnline()
    override fun setbackMenu() = PlayersMenu(player)
}

class TrollMenu(player: Player, val troll: BaseTroll, val target: Player, val category: CommandInfo.Category?) : Menu(player, true) {
    val syntax = troll.info.syntax.filter { it.purpose.isNotEmpty() }
    val params: MutableMap<Int, String> = mutableMapOf()

    init {
        for ([i, syntax] in syntax.withIndex()) {
            params[i] = syntax.default
        }
    }

    fun complete(): Boolean {
        for ([i, syntax] in syntax.withIndex()) {
            if (syntax.optional) continue
            if (!params.containsKey(i) || params[i].equals("")) return false
        }
        return true
    }

    fun item(): ItemStack {
        val item = ItemStack(if (blocked(troll, target)) Material.BARRIER else troll.info.icon)
        val meta = item.itemMeta!!
        val lore = mutableListOf<String>()

        val enabled = troll is BaseToggleTroll && troll.hasPlayer(target)
        icon(troll, meta, enabled)
        lore.addAll(listOf(typeLine(troll, enabled), "", "§7Conditions:"))
        val conditions = troll.getConditions(target)
        if (conditions != null) {
            for ([humanReadable, _, predicate] in conditions.list) {
                lore.add("§8- §${if (predicate) "c" else "a"}$humanReadable")
            }
        } else {
            lore.add(NONE)
        }

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    fun paperSlot(want: Int): Int {
        var slot = want + 12
        if (slot > 16) slot += 4
        if (slot > 25) slot += 4
        return slot.coerceIn(12, 34)
    }

    fun paramIndexFromSlot(slot: Int): Int? {
        val index = when (slot) {
            in 12..16 -> slot - 12
            in 21..25 -> slot - 16
            in 30..34 -> slot - 20
            else -> return null
        }
        return index.takeIf { it in syntax.indices }
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 45, troll.info.name)
        addItem(inventory, 10, item())

        for ([i, syntax] in syntax.withIndex()) {
            val param = params.getOrDefault(i, "")
            val item = ItemStack(Material.PAPER)
            val meta = item.itemMeta!!
            val lore = if (syntax.description.isNotEmpty())
                TypeUtil.splitAtLength(troll.info.description, 50, "§f")
            else
                mutableListOf("§fNo description.")

            lore.addAll(listOf("", "§7Type: §f${syntax.type}", "§7Optional: ${if (syntax.optional) "§aYes" else "§cNo"}"))
            if (syntax.type == CommandSyntax.Type.BOOLEAN) {
                val isTrue = param.toBoolean()
                if (isTrue) ItemUtil.makeGlow(meta)
                meta.setDisplayName("§${if (isTrue) "a" else "c"}${syntax.purpose}")
            } else {
                meta.setDisplayName("§b${syntax.purpose}")
                lore.addAll(listOf("§7Current Value", if (param.isNotEmpty()) "§f$param" else "§4None"))
            }

            meta.lore = lore
            item.itemMeta = meta
            addItem(inventory, paperSlot(i), item)
        }

        if(complete())
            addItem(inventory, 19, Material.EMERALD_BLOCK, "§aConfirm", listOf(), false)
        else
            addItem(inventory, 19, Material.BARRIER, "§4Confirm", listOf(), false)
        addItem(inventory, 28, Material.REDSTONE_BLOCK, BACK, listOf(), false)

        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (event.currentItem?.itemMeta?.displayName) {
            BACK -> TrollsMenu(player, target, category).open()
            "§aConfirm" -> {
                val args: MutableList<String> = mutableListOf()
                for (param in params.values)
                    args.add(param)
                val finalCommand = "ragebaitinator ${troll.info.name} ${target.name} ${args.joinToString(" ")}"
                player.performCommand(finalCommand)
                open()
            }
            else -> {
                val slotId = paramIndexFromSlot(event.slot) ?: return
                val field = syntax[slotId]
                if (field.type == CommandSyntax.Type.BOOLEAN) {
                    params[slotId] = (!params.getOrDefault(slotId, "false").toBoolean()).toString()
                    open()
                } else {
                    close()
                    GuiHandler.initDataInput(player, TrollGuiDataInput(this, field, slotId))
                }
            }
        }
    }

    override fun setbackCondition() = !target.isOnline()
    override fun setbackMenu() = PlayersMenu(player)
}