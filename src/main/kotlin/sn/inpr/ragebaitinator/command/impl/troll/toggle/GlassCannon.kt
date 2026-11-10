package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb
import sn.inpr.ragebaitinator.type.shooterRb

@CommandInfo(
    name = "glasscannon",
    category = CommandInfo.Category.COMBAT,
    icon = Material.TINTED_GLASS,
    description = "Hits hard, breaks easy.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class GlassCannon : BaseToggleTroll(), EntityDamageByEntitySubscriber, EntityDamageSubscriber {
    private val dealt = param<Double>("Dealt", "What the damage they deal is multiplied by (min 0.1, max 20).", default = 3.0, range = 0.1..20.0)
    private val taken = param<Double>("Taken", "What the damage they take is multiplied by (min 0.1, max 20).", default = 3.0, range = 0.1..20.0)

    override val syntax = withTarget(dealt, taken)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val dealt = args[dealt]
        val taken = args[taken]
        setField(target, "dealt", dealt)
        setField(target, "taken", taken)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now deals %c1%%dealt%x %c2%damage and takes %c1%%taken%x", target)
            .put("dealt", dealt.toString().removeSuffix(".0"))
            .put("taken", taken.toString().removeSuffix(".0"))
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer a glass cannon", target)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager.asRb ?: (event.damager as? Projectile)?.shooterRb ?: return
        if (event.isCancelled || !hasPlayer(attacker))
            return
        event.damage *= getField<Double>(attacker, "dealt") ?: 3.0
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val victim = event.entity.asRb ?: return
        if (event.isCancelled || !hasPlayer(victim))
            return
        event.damage *= getField<Double>(victim, "taken") ?: 3.0
    }
}
