package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "starve",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.ROTTEN_FLESH,
    description = "Starves a player. Yeah.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Starve : BaseToggleTroll() {
    private val ticks = param<Long>("Ticks", "The amount of ticks per second to decrease the food level by 1. Set to 0 to set hunger to 0 instantly.", default = 0L, range = 0L..60L)

    override val syntax = withTarget(ticks)

    override fun getConditions(target: RbPlayer) = Conditions()
        .notDead(target)
        .put("This player must not already being starving.", "hunger", target.foodLevel <= 0)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val ticks = args[ticks]
        return if (ticks > 0) {
            setTaskTimer(target, {
                if (target.saturation > 0)
                    target.saturation = 0f

                if (target.foodLevel > 0 && target.health > 0)
                    target.foodLevel--
                else
                    removePlayer(target, RemoveReason.DONE)
            }, 0, ticks)
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

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s hunger is no longer being decreased", target)
            .toString()
}