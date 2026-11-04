package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerVelocityEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerVelocitySubscriber
import java.util.UUID

@CommandInfo(
    name = "velocity",
    description = "Magnifies a player's velocity when they take damage.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Multiplier",
            description = "The multiplier to apply to the player's velocity",
            default = "2"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Velocity : BaseToggleTroll(), EntityDamageSubscriber, PlayerVelocitySubscriber {
    private val multipliers = mutableMapOf<UUID, Double>()
    private val damaged = mutableSetOf<UUID>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val multiplier = args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0, 5.0) ?: 2.0
        multipliers[target.uniqueId] = multiplier
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has built-in %c1%%multiplier%x %c2%velocity hacks")
            .put("player", target.name)
            .put("multiplier", multiplier.toString())
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%'s velocity is no longer being magnified")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        multipliers.remove(player.uniqueId)
        damaged.remove(player.uniqueId)
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            // velocity is usually dealt after damage
            damaged.add(player.uniqueId)

            // in case a player somehow doesn't get removed from the damaged set
            addTask(player, { damaged.remove(player.uniqueId) }, 20)
        }
    }

    override fun onPlayerVelocity(event: PlayerVelocityEvent) {
        if (players.containsKey(event.player.uniqueId) && damaged.remove(event.player.uniqueId)) {
            val multiplier = multipliers[event.player.uniqueId] ?: 2.0 // saftey
            event.velocity = event.velocity.multiply(multiplier)
            removeTask(event.player.uniqueId)
        }
    }
}