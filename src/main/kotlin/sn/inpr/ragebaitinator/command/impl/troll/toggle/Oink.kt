package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.utility.TextTransform

@CommandInfo(
    name = "oink",
    category = CommandInfo.Category.CHAT,
    icon = Material.PORKCHOP,
    description = "Ig-pay Atin-lay",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "uwu", "garble" ]
)
class Oink : BaseChatTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking in pig latin")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer speaking in pig latin")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.pigLatin(message)
}
