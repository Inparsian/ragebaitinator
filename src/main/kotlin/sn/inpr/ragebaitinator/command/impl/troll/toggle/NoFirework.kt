package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.PlayerUtil.isFireworkBoost

@CommandInfo(
    name = "nofirework",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.GUNPOWDER,
    description = "Makes a player unable to firework boost while gliding.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoFirework : BaseToggleTroll(), PlayerInteractSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer firework boost")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now firework boost again")
            .put("player", target.name)
            .toString()

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        event.cancelIf(hasPlayer(event.player) && isFireworkBoost(event))
    }
}