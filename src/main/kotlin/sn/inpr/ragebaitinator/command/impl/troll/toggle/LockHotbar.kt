package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemHeldSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerSwapHandItemsSubscriber

@CommandInfo(
    name = "lockhotbar",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.TRIPWIRE_HOOK,
    description = "Stuck with what you've got. Prevents a player from changing their held item.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class LockHotbar : BaseToggleTroll(), PlayerItemHeldSubscriber, PlayerSwapHandItemsSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now stuck with the item they're holding")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can change their held item again")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemHeld(event: PlayerItemHeldEvent) = event.cancelIf(hasPlayer(event.player))
    override fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) = event.cancelIf(hasPlayer(event.player))
}
