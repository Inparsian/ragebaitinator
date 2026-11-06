package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityRegainHealthEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityRegainHealthSubscriber

@CommandInfo(
    name = "noheal",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.GLISTERING_MELON_SLICE,
    description = "It's like always being low on hunger!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoHeal : BaseToggleTroll(), EntityRegainHealthSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer regenerate health")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now regenerate health again")
            .put("player", target.name)
            .toString()

    override fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        event.cancelIf(hasPlayer(event.entity as? Player ?: return))
    }
}
