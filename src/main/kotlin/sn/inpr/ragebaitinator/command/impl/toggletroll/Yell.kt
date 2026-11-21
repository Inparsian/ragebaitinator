package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "yell",
    category = CommandInfo.Category.CHAT,
    icon = Material.GOAT_HORN,
    // i swear to god this was in minecraft's community standards verbatim at one point
    // this could not be more perfect
    description = "Also, all-caps is often read as YELLING! And yelling at someone is usually not very nice.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "stutter", "pirate", "haxorman", "uwu", "oink", "garble", "mock", "leet", "clap", "backwards", "censor" ]
)
class Yell : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now YELLING", target)

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has calmed down and is no longer yelling", target)

    override fun transform(player: Player, message: String): String = message.uppercase()
}
