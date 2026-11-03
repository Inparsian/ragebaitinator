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
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Harmless : BaseToggleTroll(), EntityDamageByEntitySubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = "&c${target.name} &fis now totally harmless :)"

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fis no longer harmless"

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (players.containsKey(player.uniqueId))
            event.setDamage(0.0)
    }
}