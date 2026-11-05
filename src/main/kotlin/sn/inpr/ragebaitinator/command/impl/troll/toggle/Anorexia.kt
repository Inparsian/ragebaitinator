package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber

@CommandInfo(
    name = "anorexia",
    description = "Makes a player unable to eat (also makes gapples ineffective).",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Anorexia : BaseToggleTroll(), PlayerItemConsumeSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%has now completely lost their appetite")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now eat again")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (players.containsKey(event.player.uniqueId))
            event.isCancelled = true
    }
}