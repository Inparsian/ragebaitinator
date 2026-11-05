package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityToggleGlideEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityToggleGlideSubscriber

@CommandInfo(
    name = "noelytra",
    description = "Makes a player unable to glide with an elytra.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoElytra : BaseToggleTroll(), EntityToggleGlideSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        target.isGliding = false
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer glide with an elytra")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now glide with an elytra again")
            .put("player", target.name)
            .toString()

    override fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId))
            event.isCancelled = true
    }
}
