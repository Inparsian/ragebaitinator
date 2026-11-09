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
    name = "censor",
    category = CommandInfo.Category.CHAT,
    icon = Material.BLACK_DYE,
    description = "###############",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Chance",
            description = "The chance (0 to 1) of each word being censored.",
            default = "0.3"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "uwu", "oink", "garble", "mock", "leet", "clap", "backwards", "yell" ]
)
class Censor : BaseChatTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        setField(target, "chance", args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.3)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being censored")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer being censored")
            .put("player", target.name)
            .toString()

    override fun transform(player: Player, message: String): String
        = TextTransform.censor(message, getField<Double>(player, "chance") ?: 0.3)
}
