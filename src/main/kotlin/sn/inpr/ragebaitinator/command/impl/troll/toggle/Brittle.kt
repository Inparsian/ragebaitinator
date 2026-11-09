package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "brittle",
    category = CommandInfo.Category.COMBAT,
    icon = Material.GLASS,
    description = "Kills a player next time they take damage.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Brittle : BaseToggleTroll(), EntityDamageSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now die next time they take damage", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer die next time they take damage", target)
            .toString()

    override fun onEntityDamage(event: EntityDamageEvent) {
        if (removePlayer(event.entity.asRb ?: return))
            event.damage = PlayerUtil.MAX_POSSIBLE_DAMAGE
    }
}