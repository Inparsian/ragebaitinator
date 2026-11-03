package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "cannon",
    description = "Shoots a primed TNT of specified power & speed",
    syntax = [
        CommandSyntax(
            type = CommandSyntax.Type.FLOAT,
            purpose = "Power",
            description = "The power of the TNT",
            default = "0"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.FLOAT,
            purpose = "Speed",
            description = "The speed of the TNT",
            default = "1"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Ticks",
            description = "The amount of ticks before the TNT explodes",
            default = "80"
        )
    ]
)
class Cannon : BaseCommand() {
    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val power = args.getOrNull(1)?.toFloatOrNull() ?: 0f
        val speed = args.getOrNull(2)?.toFloatOrNull() ?: 1f
        val ticks = args.getOrNull(3)?.toIntOrNull() ?: 80

        if (sender is Player) {
            val tnt = sender.world.spawn(sender.location, TNTPrimed::class.java)
            tnt.velocity = sender.location.direction.multiply(speed)
            tnt.yield = power
            tnt.fuseTicks = ticks
        } else {
            send(sender, "&cYou must be a player to use this command")
        }

        return true
    }
}