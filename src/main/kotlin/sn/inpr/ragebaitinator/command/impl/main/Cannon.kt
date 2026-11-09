package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "cannon",
    description = "Shoots a primed TNT of specified power & speed",
)
class Cannon : BaseCommand() {
    private val power = param<Float>("Power", "The power of the TNT", default = 0f)
    private val speed = param<Float>("Speed", "The speed of the TNT", default = 1f)
    private val ticks = param<Int>("Ticks", "The amount of ticks before the TNT explodes", default = 80)

    override val syntax = listOf(power, speed, ticks)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        val power = args[power]
        val speed = args[speed]
        val ticks = args[ticks]

        if (sender is Player) {
            val tnt = sender.world.spawn(sender.location, TNTPrimed::class.java)
            tnt.velocity = sender.location.direction.multiply(speed)
            tnt.yield = power
            tnt.fuseTicks = ticks
        } else {
            send(sender, Placeholders(language.mustBeAPlayer))
        }

        return true
    }
}