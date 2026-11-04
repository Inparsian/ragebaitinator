package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "burn",
    description = "Phew; it's starting to get hot in here.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Ticks",
            description = "How long the player should be on fire",
            default = "100"
        )
    ]
)
class Burn : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val ticks: Int = (args.getOrNull(2)?.toIntOrNull() ?: 100).coerceIn(20, 1200)
        target.fireTicks = ticks
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been set on fire for %c1%$ticks %c2%tick(s)!")
            .put("player", target.name)
            .toString()
    }
}