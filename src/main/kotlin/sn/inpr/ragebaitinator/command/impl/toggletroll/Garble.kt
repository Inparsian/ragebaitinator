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
    name = "garble",
    category = CommandInfo.Category.CHAT,
    icon = Material.WRITABLE_BOOK,
    description = "Now they maek even less senes!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "stutter", "pirate", "haxorman", "uwu", "oink", "mock", "leet", "clap", "backwards", "yell", "censor" ]
)
class Garble : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player%%c2%'s messages are now incoherent", target)
            
    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s messages make sense again", target)
            
    override fun transform(player: Player, message: String): String = TextTransform.garble(message)
}
