package sn.inpr.ragebaitinator.command.impl.troll.toggle

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerBedEnterEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerBedEnterSubscriber

@CommandInfo(
    name = "insomnia",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.WHITE_BED,
    description = "Prevents a player from sleeping.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Insomnia : BaseToggleTroll(), PlayerBedEnterSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String =
        placeholdersOr("init", "%prefix% %c1%%player% %c2%is now an insomniac")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer an insomniac")
            .put("player", target.name)
            .toString()

    override fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        event.cancelIfAndThen(hasPlayer(event.player)) {
            val text = placeholdersOr("message", "No matter how hard you try, you can't seem to rest")
                .put("player", event.player.name)
                .toString()
            event.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(text))
        }
    }
}