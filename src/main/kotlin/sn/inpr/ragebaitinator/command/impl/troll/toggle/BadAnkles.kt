package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffectType
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "badankles",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.BONE,
    description = "Makes a player more susceptible to fall damage (yes, jumping will cause damage)",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class BadAnkles : BaseToggleTroll(), PlayerMoveSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now on Life Alert", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer on Life Alert", target)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (hasPlayer(event.player)) {
            // jump boost will raise the minimum fall distance to take fall damage
            val jumpAmplifier = event.player.getPotionEffect(PotionEffectType.JUMP_BOOST)?.amplifier?.plus(1f) ?: 0f
            val minFallDistance = 4f + jumpAmplifier

            if (event.player.fallDistance in 0.5f..<minFallDistance)
                event.player.fallDistance = minFallDistance
        }
    }
}