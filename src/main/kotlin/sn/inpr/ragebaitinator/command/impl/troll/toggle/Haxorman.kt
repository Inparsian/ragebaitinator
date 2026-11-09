package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.TextTransform

@CommandInfo(
    name = "haxorman",
    category = CommandInfo.Category.CHAT,
    icon = Material.COMMAND_BLOCK,
    description = "I'm in. Turns a player's chat messages into MD5 hashes.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "uwu", "oink", "garble", "mock", "leet", "clap", "backwards", "yell", "censor" ]
)
class Haxorman : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking in salted MD5 hashes")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has stopped hashing their messages")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String = TextTransform.md5(message)
}
