package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb
import sn.inpr.ragebaitinator.type.shooterRb

@CommandInfo(
    name = "boomhit",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.DIAMOND_SWORD,
    description = "Makes the next (or every) entity a player hits explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomHit : BaseBoomTroll(), EntityDamageByEntitySubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player hits something?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% entity %c1%%player% %c2%hits will now explode", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next entity %c1%%player% %c2%hits will no longer explode", target)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager.asRb ?: (event.damager as? Projectile)?.shooterRb ?: return
        if (event.isCancelled)
            return
        kaboom(event, player, event.entity.location, false)
    }
}
