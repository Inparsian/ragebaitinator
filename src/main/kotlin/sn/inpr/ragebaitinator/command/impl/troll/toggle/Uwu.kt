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
    name = "uwu",
    category = CommandInfo.Category.CHAT,
    icon = Material.PINK_DYE,
    description = "OwO what's this? Makes a player talk in UwUspeak.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "oink", "garble", "mock", "leet", "clap", "backwards", "yell", "censor" ]
)
class Uwu : BaseChatTroll() {
    private val chance = param<Double>("Chance", "The chance (0 to 1) of each sentence ending in a tilde or an UwU.", default = 0.5, range = 0.0..1.0)

    override val syntax = withTarget(chance)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "chance", args[chance])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking in UwUspeak", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can speak like a normal person again", target)
            .toString()

    override fun transform(player: Player, message: String): String
        = TextTransform.uwu(message, getField<Double>(player, "chance") ?: 0.5)
}
