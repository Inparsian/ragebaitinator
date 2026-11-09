package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityToggleGlideEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityToggleGlideSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "noelytra",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.COBWEB,
    description = "Makes a player unable to glide with an elytra.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoElytra : BaseToggleTroll(), EntityToggleGlideSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.isGliding = false
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer glide with an elytra", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now glide with an elytra again", target)
            .toString()

    override fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        event.cancelIf(hasPlayer(event.entity as? Player ?: return))
    }
}
