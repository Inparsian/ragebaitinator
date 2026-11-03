package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.PlayerUtil.teleportRetainVelocity

@CommandInfo(
    name = "flip",
    description = "Rotates a player 180 degrees.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class Flip : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        teleportRetainVelocity(target, target.location.clone().apply { yaw += 180.0f })
        return "&c${target.name} &fhas been flipped"
    }
}
