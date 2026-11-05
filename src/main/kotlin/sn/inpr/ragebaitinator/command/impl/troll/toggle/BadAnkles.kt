package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffectType
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber

@CommandInfo(
    name = "badankles",
    description = "Makes a player more susceptible to fall damage (yes, jumping will cause damage)",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class BadAnkles : BaseToggleTroll(), PlayerMoveSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now on Life Alert")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer on Life Alert")
            .put("player", target.name)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (players.containsKey(event.player.uniqueId)) {
            // jump boost will raise the minimum fall distance to take fall damage
            val jumpAmplifier = event.player.getPotionEffect(PotionEffectType.JUMP_BOOST)?.amplifier?.plus(1f) ?: 0f
            val minFallDistance = 4f + jumpAmplifier

            if (event.player.fallDistance in 0.5f..<minFallDistance)
                event.player.fallDistance = minFallDistance
        }
    }
}