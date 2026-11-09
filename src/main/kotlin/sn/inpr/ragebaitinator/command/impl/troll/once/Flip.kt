package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "flip",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.ITEM_FRAME,
    description = "Rotates a player 180 degrees.",
)
class Flip : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.teleportRetainVelocity(target.location.clone().apply { yaw += 180.0f })
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been flipped")
            .put("player", target.name)
            .toString()
    }
}
