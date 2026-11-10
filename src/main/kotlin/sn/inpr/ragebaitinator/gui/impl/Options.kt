@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.gui.*
import sn.inpr.ragebaitinator.gui.def.Menu
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.ItemUtil

private const val CONFIRM = "confirm"
private const val RESET = "reset"

private enum class Kind { BOOLEAN, NUMBER, CHOICE, TEXT }

private val numbers = setOf(Int::class, Long::class, Float::class, Double::class)

private fun kindOf(parameter: Parameter<*>) = when {
    parameter.type == Boolean::class -> Kind.BOOLEAN
    parameter.type in numbers -> Kind.NUMBER
    parameter.strictCompletion || parameter.argumentType.values.isNotEmpty() -> Kind.CHOICE
    else -> Kind.TEXT
}

private fun bound(value: Any?): String? = (value as? Number)?.toString()?.removeSuffix(".0")

class TrollOptionsMenu(
    player: Player,
    val troll: BaseTroll,
    val selection: Selection,
    val back: () -> Menu,
) : Menu(player), TrollToggleSubscriber, PlayerQuitSubscriber {
    private val parameters = troll.syntax.filter { it.purpose.isNotEmpty() }
    private val values = parameters.map { it.defaultText }.toMutableList()
    private val slots = centeredSlots(parameters.size, 9)

    private fun complete() = parameters.indices.all { parameters[it].optional || values[it].isNotEmpty() }

    private fun range(parameter: Parameter<*>) = (parameter as? Param<*>)?.range

    override fun title() = GuiText.text("gui.parameters.title", "troll" to troll.info.name, "target" to selection.label)

    private fun infoItem(): ItemStack {
        val enabled = enabledOn(troll, selection)
        val reasons = blockedReasons(troll, selection)

        val item = ItemStack(if (reasons.isNotEmpty()) Material.BARRIER else troll.info.icon)
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)
        if (enabled.isNotEmpty()) ItemUtil.makeGlow(meta)
        meta.setDisplayName(trollName(troll, enabled.size, selection.players.size))

        val lore = GuiText.wrap(troll.description).toMutableList()
        lore.add("")
        lore.add(stateLine(troll, enabled.size, selection.players.size))
        if (reasons.isNotEmpty()) {
            lore.add(GuiText.text("gui.troll.blocked.header"))
            lore.addAll(reasons.take(5))
        }
        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    private fun parameterItem(index: Int): ItemStack {
        val parameter = parameters[index]
        val value = values[index]
        val kind = kindOf(parameter)

        val item = ItemStack(
            when (kind) {
                Kind.BOOLEAN -> if (value.toBoolean()) Material.LIME_DYE else Material.GRAY_DYE
                Kind.NUMBER -> Material.COMPARATOR
                Kind.CHOICE -> Material.COMPASS
                Kind.TEXT -> Material.PAPER
            }
        )
        val meta = item.itemMeta!!
        ItemUtil.hideTooltips(meta)

        val name = troll.parameterName(parameter)
        val description = troll.parameterDescription(parameter)
        val lore = GuiText.lines(
            "gui.parameters.item.lore",
            "type" to typeName(parameter),
            "optional" to GuiText.text(if (parameter.optional) "gui.parameters.item.optional" else "gui.parameters.item.required"),
            multiline = mapOf("description" to
                if (description.isNotEmpty()) GuiText.wrap(description) else listOf(GuiText.text("gui.parameters.item.noDescription")))
        )

        val range = range(parameter)
        val min = bound(range?.start)
        val max = bound(range?.endInclusive)
        if (min != null && max != null)
            lore.addAll(GuiText.lines("gui.parameters.item.range", "min" to min, "max" to max))
        if (parameter.defaultText.isNotEmpty())
            lore.addAll(GuiText.lines("gui.parameters.item.default", "default" to parameter.defaultText))

        if (kind == Kind.BOOLEAN) {
            val isTrue = value.toBoolean()
            if (isTrue) ItemUtil.makeGlow(meta)
            meta.setDisplayName(GuiText.text(if (isTrue) "gui.parameters.item.nameTrue" else "gui.parameters.item.nameFalse", "name" to name))
        } else {
            meta.setDisplayName(GuiText.text("gui.parameters.item.name", "name" to name))
            lore.addAll(
                if (value.isNotEmpty()) GuiText.lines("gui.parameters.item.value", "value" to value)
                else GuiText.lines("gui.parameters.item.noValue")
            )
        }

        if (kind == Kind.CHOICE) {
            lore.add(GuiText.text("gui.parameters.item.choices"))
            for (choice in parameter.possibleValues)
                lore.add(GuiText.text(if (choice.equals(value, true)) "gui.parameters.item.choiceCurrent" else "gui.parameters.item.choice", "value" to choice))
        }

        lore.add("")
        lore.addAll(GuiText.lines("gui.parameters.controls.${kind.name.lowercase()}", "step" to (stepText(parameter) ?: "")))

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    private fun stepText(parameter: Parameter<*>): String? {
        if (parameter.type !in numbers) return null
        val range = range(parameter)
        val whole = parameter.type == Int::class || parameter.type == Long::class
        return stepFor(whole, (range?.start as? Number)?.toDouble(), (range?.endInclusive as? Number)?.toDouble())
            .stripTrailingZeros().toPlainString()
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 36, title())
        addItem(inventory, 4, infoItem())

        for (index in parameters.indices)
            slots.getOrNull(index)?.let { addItem(inventory, it, parameterItem(index)) }

        addItem(inventory, 27, Material.REDSTONE_BLOCK, GuiText.text("gui.common.back.name"), listOf(), false, BACK)
        if (parameters.isNotEmpty())
            addItem(inventory, 29, Material.LAVA_BUCKET, GuiText.text("gui.parameters.reset.name"), GuiText.lines("gui.parameters.reset.lore"), false, RESET)

        if (complete()) {
            val command = commandLine(troll.info.name, targetsFor(troll, selection).joinToString(","), values).removePrefix("ragebaitinator ")
            addItem(
                inventory, 31, Material.EMERALD_BLOCK, GuiText.text("gui.parameters.confirm.name"),
                GuiText.lines("gui.parameters.confirm.lore", "command" to command), false, CONFIRM
            )
        } else {
            addItem(inventory, 31, Material.BARRIER, GuiText.text("gui.parameters.confirmBlocked.name"), GuiText.lines("gui.parameters.confirmBlocked.lore"), false)
        }

        frame(inventory)
        return inventory
    }

    private fun change(index: Int, value: String) {
        values[index] = value
        update()
    }

    private fun normalize(parameter: Parameter<*>, text: String): String {
        if (kindOf(parameter) != Kind.NUMBER && kindOf(parameter) != Kind.CHOICE)
            return text
        val parsed = parameter.parse(text, player) ?: return text
        @Suppress("UNCHECKED_CAST")
        return (parameter.argumentType as ArgumentType<Any>).format(parsed)
    }

    private fun rejection(parameter: Parameter<*>, text: String, who: Player): Placeholders? {
        val closed = parameter.strictCompletion || parameter.argumentType.values.isNotEmpty()
        if (closed
            && parameter.possibleValues.none { it.equals(text, true) }
            && parameter.completionAliases.none { it.equals(text, true) }
            && parameter.argumentType.aliases.none { it.equals(text, true) })
            return Placeholders("gui.messages.fieldInvalidCompletion", "%prefix% &cThis value must be one of the possible values listed above")

        // a value that can't be read isn't one the command would be able to use
        if (parameter.parse(text, who) == null)
            return if (parameter.type == Targets::class)
                Placeholders("gui.messages.fieldInvalidPlayer", "%prefix% &cPlayer(s) not found")
            else
                Placeholders("gui.messages.fieldInvalid", "%prefix% &cInvalid %type%").put("type", parameter.argumentType.key)

        return null
    }

    private fun typeIn(index: Int) {
        val parameter = parameters[index]
        close()
        GuiHandler.ask(player, ChatInput(
            this, troll.parameterName(parameter), parameter.possibleValues,
            rejection = { text, who -> rejection(parameter, text, who) }
        ) { text ->
            val value = normalize(parameter, text)
            values[index] = value
            send(player, Placeholders("gui.messages.fieldSet", "%prefix% %c2%Set %c1%%field% %c2%to %c1%%value%")
                .put("field", parameter.purpose).put("value", value))
        })
    }

    private fun clickParameter(event: InventoryClickEvent, index: Int) {
        val parameter = parameters[index]
        val drop = event.click == ClickType.DROP || event.click == ClickType.CONTROL_DROP
        if (!event.isLeftClick && !event.isRightClick && !drop)
            return

        val direction = if (event.isLeftClick) 1 else -1
        when (kindOf(parameter)) {
            Kind.BOOLEAN -> change(index, (!values[index].toBoolean()).toString())
            Kind.NUMBER -> if (drop) typeIn(index) else {
                val range = range(parameter)
                stepNumber(
                    parameter.type, values[index], direction, event.isShiftClick,
                    (range?.start as? Number)?.toDouble(), (range?.endInclusive as? Number)?.toDouble()
                )?.let { change(index, it) }
            }
            Kind.CHOICE -> if (!drop) change(index, cycle(parameter.possibleValues, values[index], direction))
            Kind.TEXT -> typeIn(index)
        }
    }

    override fun onClick(event: InventoryClickEvent) {
        when (actionOf(event.currentItem)) {
            BACK -> back().open()
            CONFIRM -> runTroll(player, troll, selection, values)
            RESET -> {
                parameters.forEachIndexed { index, parameter -> values[index] = parameter.defaultText }
                update()
            }
            else -> clickParameter(event, slots.indexOf(event.slot).takeIf { it >= 0 } ?: return)
        }
    }

    override fun setbackCondition() = selection.isGone
    override fun setbackMenu() = PlayersMenu(player)

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        if (event.player.name in selection.names)
            runTaskLater({ if (selection.isGone) setbackMenu().open() else update() }, 2)
    }
}
