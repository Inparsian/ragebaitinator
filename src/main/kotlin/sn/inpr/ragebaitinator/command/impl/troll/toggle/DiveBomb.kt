package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "divebomb",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.PHANTOM_MEMBRANE,
    description = "Mayday, mayday!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class DiveBomb : BaseToggleTroll() {
    override fun getConditions(target: RbPlayer): Conditions? {
        return Conditions().put("This player must be gliding.", "gliding", !target.isGliding)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        addTaskTimer(target, {
            if (target.isGliding)
                target.velocity = Vector(0.0, -5.0, 0.0)
            else
                removePlayer(target, "done")
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now dive-bombing", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s flight path has been stabilized", target)
            .toString()
}