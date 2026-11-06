package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.Conditions

@CommandInfo(
    name = "starve",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.ROTTEN_FLESH,
    description = "Starves a player. Yeah.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.LONG,
            purpose = "Ticks",
            description = "The amount of ticks per second to decrease the food level by 1. Set to 0 to set hunger to 0 instantly.",
            default = "0"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Starve : BaseToggleTroll() {
    override fun getConditions(target: Player) = Conditions()
        .notDead(target)
        .put("This player must not already being starving.", "hunger", target.foodLevel <= 0)

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val ticks = args.getOrNull(2)?.toLongOrNull()?.coerceIn(0L..60L) ?: 0L
        return if (ticks > 0) {
            addTaskTimer(target, {
                if (target.saturation > 0)
                    target.saturation = 0f

                if (target.foodLevel > 0 && target.health > 0)
                    target.foodLevel--
                else
                    removePlayer(target, "done")
            }, 0, ticks, true)
            placeholdersOr("init", "%prefix% %c1%%player%%c2%'s hunger is now decreasing every %c1%%ticks% %c2%tick(s)")
                .put("player", target.name)
                .put("ticks", ticks.toString())
                .toString()
        } else {
            target.foodLevel = 0
            target.saturation = 0f
            placeholdersOr("initInstant", "%prefix% %c1%%player% %c2%is now starving to death")
                .put("player", target.name)
                .toString()
        }
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s hunger is no longer being decreased")
            .put("player", target.name)
            .toString()
}