package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityShootBowSubscriber
import sn.inpr.ragebaitinator.listeners.ProjectileLaunchSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.UUID

@CommandInfo(
    name = "loyalty",
    description = "Stop shooting yourself!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Loyalty : BaseToggleTroll(), EntityShootBowSubscriber, ProjectileLaunchSubscriber {
    val homing: MutableMap<UUID, MutableSet<Entity>> = mutableMapOf()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        homing[target.uniqueId] = mutableSetOf()
        addTaskTimer(target, {
            if (!homing.containsKey(target.uniqueId)) return@addTaskTimer

            val projectiles = homing[target.uniqueId]?.iterator() ?: return@addTaskTimer
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
        }, 0, 1, true)
        return "&fAny projectile &c${target.name} &fshoots will now home in on them"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&fAny projectile &c${target.name} &fshoots will no longer home in on them"

    override fun onEntityShootBow(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            homing[player.uniqueId]?.add(event.projectile)
        }
    }

    override fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val player = event.entity.shooter as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            homing[player.uniqueId]?.add(event.entity)

            val velocity = event.entity.velocity
            if (velocity.lengthSquared() > 0) {
                event.entity.teleport(event.entity.location.add(velocity.normalize()))
            }
        }
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        homing.remove(player.uniqueId)
    }
}