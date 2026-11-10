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
    name = "mock",
    category = CommandInfo.Category.CHAT,
    icon = Material.SPONGE,
    description = "iMiTaTiOn Is ThE sInCeReSt FoRm Of FlAtTeRy",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "stutter", "pirate", "haxorman", "uwu", "oink", "garble", "leet", "clap", "backwards", "yell", "censor" ]
)
class Mock : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now TaLkInG vErY oBnOxIoUsLy", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has now stopped whatever that was", target)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.mock(message)
}
