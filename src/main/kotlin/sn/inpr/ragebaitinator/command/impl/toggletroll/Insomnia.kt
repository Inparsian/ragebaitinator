package sn.inpr.ragebaitinator.command.impl.toggletroll

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerBedEnterEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerBedEnterSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "insomnia",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.WHITE_BED,
    description = "Prevents a player from sleeping.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Insomnia : BaseToggleTroll(), PlayerBedEnterSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now an insomniac", target)
            
    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer an insomniac", target)
            
    override fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        event.cancelIfAndThen(hasPlayer(event.player)) {
            val text = placeholdersOr("message", "No matter how hard you try, you can't seem to rest", event.player.rb)
                .toString()
            event.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(text))
        }
    }
}