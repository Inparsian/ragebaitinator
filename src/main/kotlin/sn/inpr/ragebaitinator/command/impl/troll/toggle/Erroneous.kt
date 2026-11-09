package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityShootBowSubscriber
import sn.inpr.ragebaitinator.listeners.ProjectileLaunchSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "erroneous",
    category = CommandInfo.Category.COMBAT,
    icon = Material.BOW,
    description = "That's some amazing accuracy you've got there!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Erroneous : BaseToggleTroll(), EntityShootBowSubscriber, ProjectileLaunchSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now an absolutely terrible shot")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now mysteriously hit their targets... fluke?")
            .put("player", target.name)
            .toString()

    fun drunkenVelocity(entity: Entity) {
        val multiplier = (entity.velocity.lengthSquared() * 0.1).coerceAtLeast(0.12)
        val x = MathUtil.random(0.0, multiplier) - (multiplier / (1.5 + Math.random()))
        val y = MathUtil.random(0.0, multiplier) - (multiplier / (1.5 + Math.random()))
        val z = MathUtil.random(0.0, multiplier) - (multiplier / (1.5 + Math.random()))
        entity.velocity = entity.velocity.add(Vector(x, y, z)).multiply(MathUtil.random(0.3, 0.9))
    }

    override fun onEntityShootBow(event: EntityShootBowEvent) {
        if (hasPlayer(event.entity as? Player ?: return))
            drunkenVelocity(event.projectile)
    }

    override fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (hasPlayer(event.entity.shooter as? Player ?: return))
            drunkenVelocity(event.entity)
    }
}