package sn.inpr.ragebaitinator.command.impl.troll.toggle

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
    description = "Hail Hydra!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Hydra : BaseToggleTroll(), EntityDeathSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = "&fAny mob &c${target.name} &fkills shalt now split into two"

    override fun deinit(sender: CommandSender, target: Player): String
        = "&fAny mob &c${target.name} &fkills shalt no longer split into two"

    override fun onEntityDeath(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        if (players.containsKey(player.uniqueId)) {
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