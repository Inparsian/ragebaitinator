package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.Parameter
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.commandLine
import sn.inpr.ragebaitinator.gui.def.Selection
import sn.inpr.ragebaitinator.gui.togglePlan
import sn.inpr.ragebaitinator.type.rb

const val TROLL = "troll:"

fun permittedTrolls(player: Player): List<BaseTroll> =
    CommandManager.trolls.filter { player.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}") }

fun hasOptions(troll: BaseTroll) = troll.syntax.any { it.purpose.isNotEmpty() }

fun runsWithDefaults(troll: BaseTroll) = troll.syntax.drop(1).all { it.optional }

fun typeName(parameter: Parameter<*>) = GuiText.text("gui.types.${parameter.argumentType.key}")

fun enabledOn(troll: BaseTroll, selection: Selection): Set<String> =
    if (troll is BaseToggleTroll) selection.players.filter { troll.hasPlayer(it) }.map { it.name }.toSet() else emptySet()

fun trollName(troll: BaseTroll, on: Int, total: Int) = GuiText.text(
    when {
        troll !is BaseToggleTroll -> "gui.troll.name.once"
        on == 0 -> "gui.troll.name.disabled"
        on >= total -> "gui.troll.name.enabled"
        else -> "gui.troll.name.partial"
    },
    "troll" to troll.info.name
)

fun stateLine(troll: BaseTroll, on: Int, total: Int) = if (troll is BaseToggleTroll)
    GuiText.text(
        "gui.troll.type.toggle",
        "state" to when {
            on == 0 -> GuiText.text("gui.troll.state.disabled")
            on >= total -> GuiText.text("gui.troll.state.enabled")
            else -> GuiText.text("gui.troll.state.partial", "on" to on.toString(), "total" to total.toString())
        }
    )
else
    GuiText.text("gui.troll.type.once")

fun blockedReasons(troll: BaseTroll, selection: Selection): List<String> {
    val lines = mutableListOf<String>()
    for (target in selection.players) {
        // this is the order that the command itself checks them in
        val reason = if (target.rb.isImmune(troll))
            GuiText.text("gui.troll.blocked.immune")
        else
            troll.getConditions(target)?.list?.firstOrNull { it.predicate }?.humanReadable
        if (reason == null)
            continue

        lines.add(
            if (selection.isMany) GuiText.text("gui.troll.blocked.many", "player" to target.name, "reason" to reason)
            else GuiText.text("gui.troll.blocked.line", "reason" to reason)
        )
    }
    return lines
}

fun targetsFor(troll: BaseTroll, selection: Selection): List<String> {
    val names = selection.players.map { it.name }
    return if (troll is BaseToggleTroll) togglePlan(names, enabledOn(troll, selection)) else names
}

fun runTroll(player: Player, troll: BaseTroll, selection: Selection, arguments: List<String> = emptyList()) {
    val targets = targetsFor(troll, selection)
    if (targets.isEmpty())
        return

    player.performCommand(commandLine(troll.info.name, targets.joinToString(","), arguments))
}
