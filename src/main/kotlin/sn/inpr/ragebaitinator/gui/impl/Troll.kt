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
import sn.inpr.ragebaitinator.gui.*
import sn.inpr.ragebaitinator.utility.ItemUtil
import sn.inpr.ragebaitinator.utility.MathUtil

private const val PAGE_SIZE = 36
private const val TROLL = "troll:"
private const val CATEGORY = "category:"
private const val ALL = "all"
private const val CONFIRM = "confirm"

private fun trollsIn(category: CommandInfo.Category?): List<BaseTroll>
    = CommandManager.trolls.filter { category == null || it.info.category == category }.sortedBy { it.info.name }

private fun blocked(troll: BaseTroll, target: Player) = troll.getConditions(target)?.list?.any { it.predicate } == true

// the first parameter of a troll is always the player it's used on, so it's everything after that which has to be
// filled in, if any of it is required. this is what lets a troll be run with all of its defaults in one click
private fun runsWithDefaults(command: BaseCommand) = command.info.syntax.drop(1).all { it.optional }

private fun trollName(troll: BaseTroll, enabled: Boolean) = GuiText.text(
    when {
        troll !is BaseToggleTroll -> "gui.troll.name.once"
        enabled -> "gui.troll.name.enabled"
        else -> "gui.troll.name.disabled"
    },
    "troll" to troll.info.name
)

// a toggle glows while it's on, and the name of one says whether it is. what is clicked is kept on the item, since the
// name of it can be anything
private fun icon(troll: BaseTroll, meta: ItemMeta, enabled: Boolean) {
    ItemUtil.hideTooltips(meta)
    if (troll is BaseToggleTroll && enabled) ItemUtil.makeGlow(meta)
    meta.setDisplayName(trollName(troll, enabled))
    Menu.tag(meta, TROLL + troll.info.name)
}

private fun typeLine(troll: BaseTroll, enabled: Boolean) = if (troll is BaseToggleTroll)
    GuiText.text("gui.troll.type.toggle", "state" to GuiText.text(if (enabled) "gui.troll.state.enabled" else "gui.troll.state.disabled"))
else
    GuiText.text("gui.troll.type.once")

private fun typeName(type: CommandSyntax.Type) = GuiText.text("gui.types.${type.name.lowercase()}")

private fun conditionLines(troll: BaseTroll, target: Player): List<String> {
    val conditions = troll.getConditions(target)?.list
    if (conditions.isNullOrEmpty())
        return listOf(GuiText.text("gui.troll.conditions.none"))

    return conditions.map { [humanReadable, _, predicate] ->
        GuiText.text(if (predicate) "gui.troll.conditions.unmet" else "gui.troll.conditions.met", "condition" to humanReadable)
    }
}

private fun parameterLines(troll: BaseTroll): List<String> {
    val parameters = troll.info.syntax.filter { it.purpose.isNotEmpty() }
    if (parameters.isEmpty())
        return listOf(GuiText.text("gui.troll.parameters.none"))

    return parameters.map { syntax ->
        val default = when {
            syntax.default.isEmpty() -> ""
            syntax.type == CommandSyntax.Type.BOOLEAN -> GuiText.text(
                if (syntax.default.toBoolean()) "gui.troll.parameters.defaultTrue" else "gui.troll.parameters.defaultFalse",
                "value" to syntax.default
            )
            else -> GuiText.text("gui.troll.parameters.default", "value" to syntax.default)
        }

        GuiText.text(
            "gui.troll.parameters.line",
            "name" to troll.parameterName(syntax),
            "type" to typeName(syntax.type),
            "default" to default,
            "required" to if (syntax.optional) "" else GuiText.text("gui.troll.parameters.required")
        )
    }
}

class TrollCategoriesMenu(player: Player, val target: Player) : Menu(player, true) {
    private companion object {
        val CATEGORY_SLOTS = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24)
        const val BACK_SLOT = 27
        const val ALL_SLOT = 31
    }

    private fun categoryItem(category: CommandInfo.Category): ItemStack? {
        val trolls = CommandManager.trolls.filter { it.info.category == category }
        if (trolls.isEmpty())
            return null

        val toggles = trolls.filterIsInstance<BaseToggleTroll>()
        val enabled = toggles.count { it.hasPlayer(target) }

        val item = ItemStack(category.icon)
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)
        meta.setDisplayName(GuiText.text("gui.categories.item.name", "category" to category.localName))
        meta.lore = GuiText.lines(
            "gui.categories.item.lore",
            "count" to trolls.size.toString(),
            "enabled" to enabled.toString(),
            "toggles" to toggles.size.toString(),
            "player" to target.name,
            multiline = mapOf("description" to GuiText.wrap(category.localDescription))
        )
        if (enabled > 0) ItemUtil.makeGlow(meta)
        tag(meta, CATEGORY + category.key)
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 36, GuiText.text("gui.categories.title"))
        val items = CommandInfo.Category.entries.mapNotNull { categoryItem(it) }
        items.zip(CATEGORY_SLOTS).forEach { [item, slot] -> addItem(inventory, slot, item) }

        addItem(
            inventory, ALL_SLOT, Material.NETHER_STAR, GuiText.text("gui.categories.all.name"),
            GuiText.lines("gui.categories.all.lore", "count" to CommandManager.trolls.size.toString()), false, ALL
        )
        addItem(inventory, BACK_SLOT, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (val action = actionOf(event.currentItem)) {
            BACK -> PlayerMenu(player, target).open()
            ALL -> TrollsMenu(player, target, null).open()
            else -> CommandInfo.Category.entries.firstOrNull { action == CATEGORY + it.key }
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
    fun trollItem(troll: BaseTroll): ItemStack {
        val item = ItemStack(if (blocked(troll, target)) Material.BARRIER else troll.info.icon)
        val meta = item.itemMeta!!
        val enabled = troll is BaseToggleTroll && troll.hasPlayer(target)
        icon(troll, meta, enabled)

        val lore = GuiText.wrap(troll.description).toMutableList()
        lore.addAll(listOf("", typeLine(troll, enabled), "", GuiText.text("gui.troll.conditions.header")))
        lore.addAll(conditionLines(troll, target))
        lore.addAll(listOf("", GuiText.text("gui.troll.parameters.header")))
        lore.addAll(parameterLines(troll))

        val word = if (troll is BaseToggleTroll) "toggle" else "execute"
        lore.addAll(listOf("", GuiText.text("gui.troll.click.$word")))
        if (runsWithDefaults(troll))
            lore.add(GuiText.text("gui.troll.click.${word}Defaults"))

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val items: MutableList<ItemStack> = mutableListOf()
        val trolls = trollsIn(category).chunked(PAGE_SIZE)[page - 1]
        for (command in trolls)
            items.add(trollItem(command))

        val title = GuiText.text(
            "gui.trolls.title",
            "category" to (category?.localName ?: GuiText.text("gui.categories.all.title")),
            "page" to page.toString(), "pages" to pages.toString()
        )
        val inventory = Bukkit.createInventory(this, 45, title)
        inventory.contents = items.toTypedArray()
        addPaginationItems(inventory, page, pages)
        return inventory
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val command = CommandManager.getCommand(actionOf(event.currentItem)?.takeIf { it.startsWith(TROLL) }?.removePrefix(TROLL))
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
        val enabled = troll is BaseToggleTroll && troll.hasPlayer(target)
        icon(troll, meta, enabled)

        val lore = mutableListOf(typeLine(troll, enabled), "", GuiText.text("gui.troll.conditions.header"))
        lore.addAll(conditionLines(troll, target))
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
        val inventory = Bukkit.createInventory(this, 45, GuiText.text("gui.parameters.title", "troll" to troll.info.name))
        addItem(inventory, 10, item())

        for ([i, syntax] in syntax.withIndex()) {
            val param = params.getOrDefault(i, "")
            val item = ItemStack(Material.PAPER)
            val meta = item.itemMeta!!
            val name = troll.parameterName(syntax)
            val description = troll.parameterDescription(syntax)
            val lore = GuiText.lines(
                "gui.parameters.item.lore",
                "type" to typeName(syntax.type),
                "optional" to GuiText.text(if (syntax.optional) "gui.parameters.item.optional" else "gui.parameters.item.required"),
                multiline = mapOf("description" to
                    if (description.isNotEmpty()) GuiText.wrap(description) else listOf(GuiText.text("gui.parameters.item.noDescription")))
            )

            if (syntax.type == CommandSyntax.Type.BOOLEAN) {
                val isTrue = param.toBoolean()
                if (isTrue) ItemUtil.makeGlow(meta)
                meta.setDisplayName(GuiText.text(if (isTrue) "gui.parameters.item.nameTrue" else "gui.parameters.item.nameFalse", "name" to name))
            } else {
                meta.setDisplayName(GuiText.text("gui.parameters.item.name", "name" to name))
                lore.addAll(
                    if (param.isNotEmpty()) GuiText.lines("gui.parameters.item.value", "value" to param)
                    else GuiText.lines("gui.parameters.item.noValue")
                )
            }

            meta.lore = lore
            item.itemMeta = meta
            addItem(inventory, paperSlot(i), item)
        }

        if (complete())
            addItem(inventory, 19, Material.EMERALD_BLOCK, GuiText.text("gui.parameters.confirm.name"), listOf(), false, CONFIRM)
        else
            addItem(inventory, 19, Material.BARRIER, GuiText.text("gui.parameters.confirmBlocked.name"), listOf(), false)
        addItem(inventory, 28, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)

        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (actionOf(event.currentItem)) {
            BACK -> TrollsMenu(player, target, category).open()
            CONFIRM -> {
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
