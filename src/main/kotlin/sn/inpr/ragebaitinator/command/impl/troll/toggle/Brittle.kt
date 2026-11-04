package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "brittle",
    description = "Kills a player next time they take damage.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Brittle : BaseToggleTroll(), EntityDamageSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now die next time they take damage")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer die next time they take damage")
            .put("player", target.name)
            .toString()

    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            removePlayer(player)
            event.damage = PlayerUtil.MAX_POSSIBLE_DAMAGE
            runTaskLater({ if (player.isOnline && !player.isDead) {
                player.health = 0.0
            } }, 1L)
        }
    }
}