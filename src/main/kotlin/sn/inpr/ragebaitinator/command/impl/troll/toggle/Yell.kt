package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
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
    incompatible = [ "haxorman", "uwu", "oink", "garble", "mock", "leet", "clap", "backwards", "censor" ]
)
class Yell : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now YELLING")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has calmed down and is no longer yelling")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = message.uppercase()
}
