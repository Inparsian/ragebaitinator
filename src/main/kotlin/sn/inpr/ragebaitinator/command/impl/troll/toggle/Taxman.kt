package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerExpChangeEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerExpChangeSubscriber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "taxman",
    description = "Death and taxes. Takes a cut out of every XP orb a player collects.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Rate",
            description = "The fraction (0 to 1) of every XP gain that gets taxed.",
            default = "0.5"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Taxman : BaseToggleTroll(), PlayerExpChangeSubscriber {
    private val rates = ConcurrentHashMap<UUID, Double>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val rate = args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.5
        rates[target.uniqueId] = rate
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now pays %c1%%rate% %c2%income tax on all XP")
            .put("player", target.name)
            .put("rate", "${(rate * 100).toInt()}%")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been audited and no longer pays XP tax")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        rates.remove(player.uniqueId)
    }

    override fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        if (!hasPlayer(event.player) || event.amount <= 0)
            return

        // the house always wins, so this rounds down what the player gets to keep
        event.amount = (event.amount * (1.0 - (rates[event.player.uniqueId] ?: 0.5))).toInt()
    }
}
