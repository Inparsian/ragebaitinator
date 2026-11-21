package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "vampire",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.DAYLIGHT_DETECTOR,
    description = "Makes a player burn under direct sunlight.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Vampire : BaseToggleTroll(), PlayerMoveSubscriber {
    fun vampireTick(player: RbPlayer) {
        if (hasPlayer(player) && LocationUtil.getInternalSkyLight(player.location.block) >= 12)
            player.fireTicks = player.fireTicks.coerceAtLeast(100)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders {
        setTaskTimer(target, { vampireTick(target) }, 0, 10)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now combust under sunlight", target)
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer combust under sunlight", target)

    override fun onPlayerMove(event: PlayerMoveEvent) = vampireTick(event.player.rb)
}