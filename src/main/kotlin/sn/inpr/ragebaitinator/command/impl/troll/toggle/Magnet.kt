package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Enemy
import org.bukkit.entity.TNTPrimed
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "magnet",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.LODESTONE,
    description = "Attracts hostile mobs & TNT to a player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Magnet : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setTaskTimer(target, {
            for (entity in target.nearbyEntities(30.0)) {
                if (entity is Enemy || entity is TNTPrimed) {
                    val distance = target.location.distance(entity.location)
                    val velocity = target.location.toVector().subtract(entity.location.toVector())
                        .normalize()
                        .multiply((30.0 - distance) * 0.02)
                        .add(entity.velocity)
                    if (distance in 2.0..30.0) {
                        entity.velocity = velocity
                    }
                }
            }
        }, 0, 1)

        return placeholdersOr("init", "%prefix% %c2%Unpleasant entities are now being attracted to %c1%%player%", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%Unpleasant entities are no longer attracted to %c1%%player%", target)
            .toString()
}