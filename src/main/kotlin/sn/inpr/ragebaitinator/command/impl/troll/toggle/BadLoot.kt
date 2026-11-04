package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Silverfish
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerFishSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "badloot",
    description = "Makes a player catch silverfish instead of fish.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BadLoot : BaseToggleTroll(), PlayerFishSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now an extremely unlucky fisher(wo)man")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%now has regular fishing luck")
            .put("player", target.name)
            .toString()

    override fun onPlayerFish(event: PlayerFishEvent) {
        if (players.containsKey(event.player.uniqueId) && event.state == PlayerFishEvent.State.CAUGHT_FISH) {
            val caught = event.caught ?: return
            caught.remove()

            for (i in 0 until MathUtil.random(1, 3)) {
                val silverfish = event.player.world.spawn(caught.location, Silverfish::class.java)
                val velocity = event.player.location.toVector().subtract(caught.location.toVector())
                    .normalize()
                    .multiply(event.player.location.distance(caught.location) / 7.5)
                    .add(Vector(0.0, 0.55, 0.0))
                silverfish.velocity = velocity
                silverfish.target = event.player
            }
        }
    }
}