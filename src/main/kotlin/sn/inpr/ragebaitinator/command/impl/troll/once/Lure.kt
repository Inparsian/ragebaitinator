package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Creature
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "lure",
    description = "Lures every creature within a 100 block radius to a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class Lure : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        LocationUtil.getEntitiesWithinRadius(target.location, 100.0)
            .filterIsInstance<Creature>()
            .forEach { it.target = target }
        return "&c${target.name} &fis now universally hated"
    }
}
