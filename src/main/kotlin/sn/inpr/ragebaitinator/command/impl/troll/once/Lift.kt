package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.PlayerUtil.setVelocity

@CommandInfo(
    name = "lift",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.SLIME_BLOCK,
    description = "We have lift-off! Launches a player into the air",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Power",
            description = "The power of the launch.",
            default = "3"
        ),
    ]
)
class Lift : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val power = (args.getOrNull(2)?.toDoubleOrNull() ?: 3.0).coerceIn(0.1, 5.0)
        setVelocity(target, target.velocity.add(Vector(0.0, power, 0.0)))
        return placeholdersOr("init", "%prefix% %c2%Launched %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}