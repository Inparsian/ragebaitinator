package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Creature
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "lure",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.CARROT_ON_A_STICK,
    description = "Lures every creature within a 100 block radius to a player.",
)
class Lure : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        LocationUtil.getEntitiesWithinRadius(target.location, 100.0)
            .filterIsInstance<Creature>()
            .forEach { it.target = target.bukkit }
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now universally hated", target)
            .toString()
    }
}
