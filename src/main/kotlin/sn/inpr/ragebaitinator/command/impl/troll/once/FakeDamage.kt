package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "fakedamage",
    description = "Makes a player think they took damage.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class FakeDamage : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        PlayerUtil.packetDamage(target, 0.0, false)
        return "&fSent fake damage to &c${target.name}"
    }
}