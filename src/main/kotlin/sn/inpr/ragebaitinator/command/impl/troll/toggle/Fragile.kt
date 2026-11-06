package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemDamageEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemDamageSubscriber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "fragile",
    description = "Made in the cheapest factory. Makes a player's tools and armor wear out far faster.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Multiplier",
            description = "How many times faster items wear out. Set to 0 to make them break instantly.",
            default = "10"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Fragile : BaseToggleTroll(), PlayerItemDamageSubscriber {
    private companion object {
        // more than any item has, but small enough to not overflow when it's added to what's already there
        const val BREAKS_ANYTHING = 1_000_000
    }

    private val multipliers = ConcurrentHashMap<UUID, Int>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val multiplier = (args.getOrNull(2)?.toIntOrNull() ?: 10).coerceIn(0, 1000)
        multipliers[target.uniqueId] = multiplier
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s items will now %effect%")
            .put("player", target.name)
            .put("effect", if (multiplier == 0) "break instantly" else "wear out ${multiplier}x faster")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s items now wear out normally")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        multipliers.remove(player.uniqueId)
    }

    override fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        val multiplier = multipliers[event.player.uniqueId] ?: return
        if (event.isCancelled || !hasPlayer(event.player))
            return

        event.damage = if (multiplier == 0) BREAKS_ANYTHING else (event.damage * multiplier).coerceAtMost(BREAKS_ANYTHING)
    }
}
