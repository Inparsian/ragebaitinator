package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "thorns",
    category = CommandInfo.Category.COMBAT,
    icon = Material.CACTUS,
    description = "Makes any damage a player deals return to them.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "harmless" ]
)
class Thorns : BaseToggleTroll(), EntityDamageByEntitySubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now take damage when they hit someone", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer take damage when they hit someone", target)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        if (hasPlayer(player)) {
            val damage = event.damage
            event.setDamage(0.0)
            val cause = EntityDamageEvent.DamageCause.THORNS
            val source = DamageSource.builder(DamageType.THORNS)
                .withCausingEntity(event.entity)
                .withDirectEntity(event.entity)
                .withDamageLocation(event.entity.location)
                .build()
            val newEvent = EntityDamageByEntityEvent(event.entity, event.damager, cause, source, damage)
            Bukkit.getPluginManager().callEvent(newEvent)
            if (!newEvent.isCancelled)
                player.damage(damage, source)
        }
    }
}