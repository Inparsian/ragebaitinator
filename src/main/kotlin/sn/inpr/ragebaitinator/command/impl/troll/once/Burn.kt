package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "burn",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.FLINT_AND_STEEL,
    description = "Phew; it's starting to get hot in here.",
)
class Burn : BaseTroll() {
    private val ticks = param<Int>("Ticks", "How long the player should be on fire", default = 100, range = 20..1200)

    override val syntax = withTarget(ticks)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val ticks: Int = args[ticks]
        target.fireTicks = ticks
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been set on fire for %c1%$ticks %c2%tick(s)!")
            .put("player", target.name)
            .toString()
    }
}