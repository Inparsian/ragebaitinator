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
    name = "clap",
    category = CommandInfo.Category.CHAT,
    icon = Material.HONEYCOMB,
    description = "Adds *clap* claps *clap* between *clap* words *clap* for *clap* emphasis",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "uwu", "oink", "garble", "mock", "leet", "backwards", "yell", "censor" ]
)
class Clap : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%now gets applause between every word")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%no longer gets applause between words")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.clap(message)
}
