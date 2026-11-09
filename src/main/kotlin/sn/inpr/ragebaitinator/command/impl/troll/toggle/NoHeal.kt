package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityRegainHealthEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityRegainHealthSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "noheal",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.GLISTERING_MELON_SLICE,
    description = "It's like always being low on hunger!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoHeal : BaseToggleTroll(), EntityRegainHealthSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer regenerate health")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now regenerate health again")
            .put("player", target.name)
            .toString()

    override fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        event.cancelIf(hasPlayer(event.entity as? Player ?: return))
    }
}
