package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "invert",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.COMPARATOR,
    description = "Up is down, down is up... Makes sense...",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Invert : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        addTaskTimer(target, {
            if (target.flySpeed >= 0) target.flySpeed *= -1
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s fly controls are now inverted", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s fly controls are no longer inverted", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        if (player.flySpeed < 0) player.flySpeed *= -1
    }
}