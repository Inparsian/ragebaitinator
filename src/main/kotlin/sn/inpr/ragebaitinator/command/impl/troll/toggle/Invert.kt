package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "invert",
    description = "Up is down, down is up... Makes sense...",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Invert : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        addTaskTimer(target, {
            if (target.flySpeed >= 0) target.flySpeed *= -1
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s fly controls are now inverted")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s fly controls are no longer inverted")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        if (player.flySpeed < 0) player.flySpeed *= -1
    }
}