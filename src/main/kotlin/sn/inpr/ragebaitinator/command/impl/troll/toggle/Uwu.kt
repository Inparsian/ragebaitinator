package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseChatTroll
import sn.inpr.ragebaitinator.utility.TextTransform
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "uwu",
    description = "OwO what's this? Makes a player talk in UwUspeak.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Chance",
            description = "The chance (0 to 1) of each sentence ending in a tilde or an UwU.",
            default = "0.5"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "haxorman", "oink", "garble" ]
)
class Uwu : BaseChatTroll() {
    private val chances = ConcurrentHashMap<UUID, Double>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        chances[target.uniqueId] = args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.5
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now speaking in UwUspeak")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can speak like a normal person again")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        chances.remove(player.uniqueId)
    }

    override fun transform(player: Player, message: String): String
        = TextTransform.uwu(message, chances[player.uniqueId] ?: 0.5)
}
