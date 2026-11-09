package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "trip",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.DROPPER,
    description = "Makes a player drop everything in their inventory.",
)
class Trip : BaseTroll() {
    private val clone = param<Boolean>("Clone", "Should the player's inventory be cloned instead of cleared?", default = false)

    override val syntax = withTarget(clone)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val clone = args[clone]
        for (item in target.storageContents.filterNotNull()) {
            val drop = target.world.dropItemNaturally(target.location.add(0.0, 1.0, 0.0), item)
            val yaw = MathUtil.random(0.0, 360.0)
            drop.velocity = Vector(MathUtil.rcos(yaw) * 0.3, 0.2, MathUtil.rsin(yaw) * 0.3)
            if (!clone) target.removeFromInventory(item)
        }
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has inexplicably dropped all their stuff")
            .put("player", target.name)
            .toString()
    }
}