package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityResurrectEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityResurrectSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "nototem",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.TOTEM_OF_UNDYING,
    description = "Makes a player's totem(s) of undying not work.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoTotem : BaseToggleTroll(), EntityResurrectSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player%%c2%'s totems are now chinese counterfeits")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s totems will now work as advertised")
            .put("player", target.name)
            .toString()

    override fun onEntityResurrect(event: EntityResurrectEvent) {
        event.cancelIf(hasPlayer(event.entity as? Player ?: return))
    }
}
