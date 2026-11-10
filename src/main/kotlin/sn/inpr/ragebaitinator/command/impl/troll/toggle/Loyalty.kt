package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityShootBowSubscriber
import sn.inpr.ragebaitinator.listeners.ProjectileLaunchSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "loyalty",
    category = CommandInfo.Category.COMBAT,
    icon = Material.TRIDENT,
    description = "Stop shooting yourself!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Loyalty : BaseToggleTroll(), EntityShootBowSubscriber, ProjectileLaunchSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "homing", mutableSetOf<Entity>())
        setTaskTimer(target, {
            if (!hasField(target, "homing")) return@setTaskTimer

            val projectiles = getField<MutableSet<Entity>>(target, "homing")?.iterator() ?: return@setTaskTimer
            while (projectiles.hasNext()) {
                val projectile = projectiles.next()
                if (projectile.isOnGround || projectile.isDead) {
                    projectiles.remove()
                    continue
                }

                val direction = target.location.add(0.0, target.eyeHeight, 0.0)
                    .toVector()
                    .subtract(projectile.location.toVector())
                val velocity = projectile.velocity
                if (direction.lengthSquared() > 0 && velocity.lengthSquared() > 0) {
                    val speed = velocity.length()
                    projectile.velocity = velocity.multiply(0.95)
                        .add(direction.normalize().multiply(speed * 0.4))
                }
            }
        }, 0, 1)
        return placeholdersOr("init", "%prefix% %c2%Any projectile %c1%%player% %c2%shoots will now home in on them", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%Any projectile %c1%%player% %c2%shoots will no longer home in on them", target)
            .toString()

    override fun onEntityShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (hasPlayer(player))
            getField<MutableSet<Entity>>(player, "homing")?.add(event.projectile)
    }

    override fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val player = event.entity.shooter as? Player ?: return
        if (hasPlayer(player)) {
            getField<MutableSet<Entity>>(player, "homing")?.add(event.entity)

            val velocity = event.entity.velocity
            if (velocity.lengthSquared() > 0) {
                event.entity.teleport(event.entity.location.add(velocity.normalize()))
            }
        }
    }
}