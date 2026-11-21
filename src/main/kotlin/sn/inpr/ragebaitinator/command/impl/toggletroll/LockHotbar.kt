package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerItemHeldSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerSwapHandItemsSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "lockhotbar",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.TRIPWIRE_HOOK,
    description = "Stuck with what you've got. Prevents a player from changing their held item.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class LockHotbar : BaseToggleTroll(), PlayerItemHeldSubscriber, PlayerSwapHandItemsSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now stuck with the item they're holding", target)

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can change their held item again", target)

    override fun onPlayerItemHeld(event: PlayerItemHeldEvent) = event.cancelIf(hasPlayer(event.player))
    override fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) = event.cancelIf(hasPlayer(event.player))
}
