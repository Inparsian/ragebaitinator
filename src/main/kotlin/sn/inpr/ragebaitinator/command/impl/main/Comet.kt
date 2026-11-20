package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseCommand
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "comet",
    description = "Shoots a fireball of specified power & speed",
)
class Comet : BaseCommand() {
    private val power = param<Float>("Power", "The power of the fireball", default = 0f)
    private val speed = param<Float>("Speed", "The speed of the fireball", default = 1f)

    override val syntax = listOf(power, speed)

    override fun process(sender: CommandSender, command: Command, label: String, args: Args) {
        val power = args[power]
        val speed = args[speed]

        if (sender is Player) {
            val fireball = sender.world.spawn(sender.location.apply { y += sender.eyeHeight }, Fireball::class.java)
            fireball.velocity = sender.location.direction.multiply(speed)
            fireball.yield = power
        } else {
            send(sender, language.mustBeAPlayer.placeholders())
        }
    }
}