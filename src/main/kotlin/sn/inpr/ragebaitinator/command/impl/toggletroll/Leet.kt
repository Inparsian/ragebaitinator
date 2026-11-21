package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.TextTransform

@CommandInfo(
    name = "leet",
    category = CommandInfo.Category.CHAT,
    icon = Material.CHAIN_COMMAND_BLOCK,
    description = "1337",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "stutter", "pirate", "haxorman", "uwu", "oink", "garble", "mock", "clap", "backwards", "yell", "censor" ]
)
class Leet : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking 1337", target)

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer speaking leetspeak", target)

    override fun transform(player: Player, message: String): String = TextTransform.leet(message)
}
