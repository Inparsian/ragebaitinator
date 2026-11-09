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
    name = "garble",
    category = CommandInfo.Category.CHAT,
    icon = Material.WRITABLE_BOOK,
    description = "Now they maek even less senes!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "uwu", "oink", "mock", "leet", "clap", "backwards", "yell", "censor" ]
)
class Garble : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player%%c2%'s messages are now incoherent")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s messages make sense again")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.garble(message)
}
