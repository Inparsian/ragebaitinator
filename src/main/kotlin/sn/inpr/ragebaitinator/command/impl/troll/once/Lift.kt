package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "lift",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.SLIME_BLOCK,
    description = "We have lift-off! Launches a player into the air",
)
class Lift : BaseTroll() {
    private val power = param<Double>("Power", "The power of the launch.", default = 3.0, range = 0.1..5.0)

    override val syntax = withTarget(power)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val power = args[power]
        target.velocity = target.velocity.add(Vector(0.0, power, 0.0))
        return placeholdersOr("init", "%prefix% %c2%Launched %c1%%player%", target)
            .toString()
    }
}