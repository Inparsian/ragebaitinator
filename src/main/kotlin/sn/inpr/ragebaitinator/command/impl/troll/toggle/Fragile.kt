package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemDamageEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "fragile",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.WOODEN_PICKAXE,
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

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val multiplier = (args.getOrNull(2)?.toIntOrNull() ?: 10).coerceIn(0, 1000)
        setField(target, "multiplier", multiplier)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s items will now %effect%")
            .put("player", target.name)
            .put("effect", if (multiplier == 0) "break instantly" else "wear out ${multiplier}x faster")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s items now wear out normally")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        val multiplier = getField<Int>(event.player, "multiplier") ?: return
        if (event.isCancelled || !hasPlayer(event.player))
            return

        event.damage = if (multiplier == 0) BREAKS_ANYTHING else (event.damage * multiplier).coerceAtMost(BREAKS_ANYTHING)
    }
}
