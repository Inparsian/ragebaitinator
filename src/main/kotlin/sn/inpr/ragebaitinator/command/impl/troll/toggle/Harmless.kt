package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "harmless",
    category = CommandInfo.Category.COMBAT,
    icon = Material.WOODEN_SWORD,
    description = "They punch like a girl!!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "thorns" ]
)
class Harmless : BaseToggleTroll(), EntityDamageByEntitySubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now totally harmless :)")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer harmless")
            .put("player", target.name)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (hasPlayer(event.damager as? Player ?: return))
            event.setDamage(0.0)
    }
}