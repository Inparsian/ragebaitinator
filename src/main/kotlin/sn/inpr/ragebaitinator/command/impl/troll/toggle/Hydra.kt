package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDeathSubscriber
import sn.inpr.ragebaitinator.type.EntitySnapshot

@CommandInfo(
    name = "hydra",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.DRAGON_HEAD,
    description = "Hail Hydra!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Hydra : BaseToggleTroll(), EntityDeathSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c2%Any mob %c1%%player% %c2%kills shalt now split into two")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%Any mob %c1%%player% %c2%kills shalt no longer split into two")
            .put("player", target.name)
            .toString()

    override fun onEntityDeath(event: EntityDeathEvent) {
        if (hasPlayer(event.entity.killer ?: return)) {
            val snapshot = EntitySnapshot(event.entity)
            val location = event.entity.location
            val type = event.entityType
            runTaskLater({
                for (z in 0..1) {
                    val new = event.entity.world.spawnEntity(location, type)
                    snapshot.restoreOn(new)
                }
            }, 1L)
        }
    }
}