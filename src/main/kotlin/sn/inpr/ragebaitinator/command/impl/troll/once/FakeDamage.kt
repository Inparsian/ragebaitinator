package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "fakedamage",
    description = "Sends fake damage to a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class FakeDamage : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        PlayerUtil.packetDamage(target, 0.0, false)
        return placeholdersOr("init", "%prefix% %c2%Sent fake damage to %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}