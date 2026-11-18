package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "deathtag",
    category = CommandInfo.Category.CHAT,
    icon = Material.BOOK,
    description = "The next player mentioned dies, then the death tag passes to the mentioned player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class DeathTag : BaseToggleTroll(), AsyncPlayerChatSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now kill the next player they mention", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer kill the next player they mention", target)
            .toString()

    override fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (hasPlayer(event.player)) {
            val target = RbPlayer.online.firstOrNull {
                event.message.split(" ").any { word ->
                    event.player.rb != it && word.endsWith(it.name, true)
                }
            }

            if (target != null) {
                runTask { target.health = 0.0 }
                removePlayer(event.player)
                setPlayer(target)
            }
        }
    }
}
