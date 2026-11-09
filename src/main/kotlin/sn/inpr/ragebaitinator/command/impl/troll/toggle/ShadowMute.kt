package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "shadowmute",
    category = CommandInfo.Category.CHAT,
    icon = Material.NOTE_BLOCK,
    description = "Talking to yourself is the first sign.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class ShadowMute : BaseToggleTroll(), AsyncPlayerChatSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now talking to themselves")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can be heard by others again")
            .put("player", target.name)
            .toString()

    override fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!event.isCancelled && hasPlayer(event.player))
            event.recipients.removeIf { it != event.player }
    }
}
