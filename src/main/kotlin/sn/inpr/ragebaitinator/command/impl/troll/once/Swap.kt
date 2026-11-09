package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Targets
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "swap",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.ENDER_PEARL,
    description = "Swaps two players' locations (and inventories if you want to)",
)
class Swap : BaseTroll() {
    private val other = required<Targets>("Player")
    private val swapInventories = param<Boolean>("Swap Inventories", "Whether or not to swap the players' inventories", default = false)

    override val syntax = withTarget(other, swapInventories)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val target2: RbPlayer = args.getOrNull(other)?.first()
            ?: return Placeholders(language.playerNotFound).toString()
        val swapInventories: Boolean = args[swapInventories]
        val targetLocation = target.location
        target.teleport(target2.location)
        target2.teleport(targetLocation)
        if (swapInventories) {
            val contents = target.contents
            target.contents = target2.contents
            target2.contents = contents
        }
        return placeholdersOr("init", "%prefix% %c1%%playerA% %c2%and %c1%%playerB% %c2%have swapped places")
            .put("playerA", target.name)
            .put("playerB", target2.name)
            .toString()
    }
}