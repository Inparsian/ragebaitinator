package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.entity.EntityPickupItemEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityPickupItemSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "nopickup",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.HOPPER,
    description = "Makes a player unable to pick up items.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoPickup : BaseToggleTroll(), EntityPickupItemSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer pick up items", target)

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now pick up items again", target)

    override fun onEntityPickupItem(event: EntityPickupItemEvent) = event.cancelIf(hasPlayer(event.entity))
}
