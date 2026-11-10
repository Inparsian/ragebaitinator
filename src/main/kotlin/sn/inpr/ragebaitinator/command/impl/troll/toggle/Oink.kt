package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.TextTransform

@CommandInfo(
    name = "oink",
    category = CommandInfo.Category.CHAT,
    icon = Material.PORKCHOP,
    description = "Ig-pay Atin-lay",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "stutter", "pirate", "haxorman", "uwu", "garble", "mock", "leet", "clap", "backwards", "yell", "censor" ]
)
class Oink : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking in pig latin", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer speaking in pig latin", target)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.pigLatin(message)
}
