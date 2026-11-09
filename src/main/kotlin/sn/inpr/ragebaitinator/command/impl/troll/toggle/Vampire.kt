package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "vampire",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.DAYLIGHT_DETECTOR,
    description = "Makes a player burn under direct sunlight.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Vampire : BaseToggleTroll(), PlayerMoveSubscriber {
    fun vampireTick(player: RbPlayer) {
        if (hasPlayer(player) && player.location.block.lightFromSky >= 12)
            player.fireTicks = player.fireTicks.coerceAtLeast(100)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        addTaskTimer(target, { vampireTick(target) }, 0, 10, true)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now combust under sunlight")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer combust under sunlight")
            .put("player", target.name)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) = vampireTick(event.player.rb)
}