package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Animals
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "repel",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.POISONOUS_POTATO,
    description = "Why does everybody hate me? Is it because I'm ugly?",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Repel : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        addTaskTimer(target, {
            val entities = LocationUtil.getEntitiesWithinRadius(target.location, 10.0)
            for (entity in entities) {
                if (entity == target) continue
                if (entity is Player || entity is Animals) {
                    val distance = target.location.distance(entity.location)
                    val velocity = entity.location.toVector().subtract(target.location.toVector())
                        .normalize()
                        .multiply(1.2 - (distance * 0.2))
                        .add(entity.velocity)
                    if (distance < 6) {
                        entity.velocity = velocity
                    }
                }
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c2%Creatures & players are now being repelled away from %c1%%player%")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%Creatures & players are no longer repelled away from %c1%%player%")
            .put("player", target.name)
            .toString()
}