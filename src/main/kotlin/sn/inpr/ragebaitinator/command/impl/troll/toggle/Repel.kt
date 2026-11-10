package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Animals
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "repel",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.POISONOUS_POTATO,
    description = "Why does everybody hate me? Is it because I'm ugly?",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Repel : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setTaskTimer(target, {
            for (entity in target.nearbyEntities(10.0)) {
                if (entity is Player || entity is Animals) {
                    val distance = target.location.distance(entity.location)
                    val velocity = entity.location.toVector().subtract(target.location.toVector())
                        .normalize()
                        .multiply(if (distance > 0) 1.2 - (distance * 0.2) else 1.0)
                        .add(entity.velocity)
                    velocity.x = velocity.x.coerceAtMost(5.0)
                    velocity.y = velocity.y.coerceAtMost(5.0)
                    velocity.z = velocity.z.coerceAtMost(5.0)
                    if (distance < 6 && LocationUtil.isFinite(velocity)) {
                        if (entity is Player)
                            entity.rb.velocity = velocity
                        else
                            entity.velocity = velocity
                    }
                }
            }
        }, 0, 1)

        return placeholdersOr("init", "%prefix% %c2%Creatures & players are now being repelled away from %c1%%player%", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%Creatures & players are no longer repelled away from %c1%%player%", target)
            .toString()
}