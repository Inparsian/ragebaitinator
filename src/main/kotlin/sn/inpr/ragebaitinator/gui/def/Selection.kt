package sn.inpr.ragebaitinator.gui.def

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.gui.GuiText

class Selection(val names: List<String>) {
    constructor(player: Player) : this(listOf(player.name))

    val players: List<Player> get() = names.mapNotNull { Bukkit.getPlayerExact(it) }
    val isGone: Boolean get() = players.isEmpty()
    val isMany: Boolean get() = names.size > 1
    val label: String
        get() = if (names.size == 1) names.first() else GuiText.text("gui.selection.many", "count" to names.size.toString())
    val argument: String get() = players.joinToString(",") { it.name }
}
