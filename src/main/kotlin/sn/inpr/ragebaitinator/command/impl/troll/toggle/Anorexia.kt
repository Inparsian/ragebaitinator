package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "anorexia",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.BREAD,
    description = "Makes a player unable to eat (also makes gapples ineffective).",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Anorexia : BaseToggleTroll(), PlayerItemConsumeSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%has now completely lost their appetite")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now eat again")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) = event.cancelIf(hasPlayer(event.player))
}