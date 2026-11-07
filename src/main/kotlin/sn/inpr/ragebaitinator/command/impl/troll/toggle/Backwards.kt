package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll

@CommandInfo(
    name = "backwards",
    category = CommandInfo.Category.CHAT,
    icon = Material.ARROW,
    description = "ynnuf era segassem sdrawkcab ah hA",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "uwu", "oink", "garble", "mock", "leet", "clap", "yell", "censor" ]
)
class Backwards : BaseChatTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking backwards")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer speaking backwards")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = message.reversed()
}
