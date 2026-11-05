package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber

@CommandInfo(
    name = "harmless",
    description = "They punch like a girl!!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "thorns" ]
)
class Harmless : BaseToggleTroll(), EntityDamageByEntitySubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now totally harmless :)")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer harmless")
            .put("player", target.name)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (players.containsKey(player.uniqueId))
            event.setDamage(0.0)
    }
}