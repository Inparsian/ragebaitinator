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
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Anorexia : BaseToggleTroll(), PlayerItemConsumeSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = "&c${target.name} &fhas now completely lost their appetite"

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fcan now eat again"

    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (players.containsKey(event.player.uniqueId))
            event.isCancelled = true
    }
}