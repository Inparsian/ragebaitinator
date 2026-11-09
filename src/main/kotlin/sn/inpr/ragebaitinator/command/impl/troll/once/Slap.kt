package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil.vectorRelativeYaw
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "slap",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.STICK,
    description = "Slaps the player.",
)
class Slap : BaseTroll() {
    private val power = param<Double>("Power", "The power of the slap.", default = 2.0, range = 0.0..5.0)
    private val direction = param<String>(
        "Direction",
        "The direction to slap the player (cardinal, ordinal, backward/forward/left/right, crosshair, sender, random)",
        default = "backward",
        completions = listOf(
            "north", "northeast",
            "east", "southeast",
            "south", "southwest",
            "west", "northwest",
            "backward", "forward",
            "left", "right",
            "crosshair", "sender", "random"
        ),
        strictCompletion = true,
        completionAliases = listOf(
            "n", "ne", "e", "se", "s", "sw", "w", "nw",
            "b", "f", "l", "r", "me", "cross", "ch", "c",
            "rand"
        ),
    )

    override val syntax = withTarget(power, direction)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val power: Double = args[power]
        val direction = args[direction]
        val slapVector: Vector = when (direction) {
            "north", "n" -> Vector(0.0, 0.0, -power)
            "northeast", "ne" -> Vector(power, 0.0, -power)
            "east", "e" -> Vector(power, 0.0, 0.0)
            "southeast", "se" -> Vector(power, 0.0, power)
            "south", "s" -> Vector(0.0, 0.0, power)
            "southwest", "sw" -> Vector(-power, 0.0, power)
            "west", "w" -> Vector(-power, 0.0, 0.0)
            "northwest", "nw" -> Vector(-power, 0.0, -power)
            "forward", "f" -> vectorRelativeYaw(target, 0, power)
            "left", "l" -> vectorRelativeYaw(target, -90, power)
            "right", "r" -> vectorRelativeYaw(target, 90, power)
            "sender", "me" -> if (sender is Player) vectorRelativeYaw(sender, 0, power) else vectorRelativeYaw(target, 180, power)
            "crosshair", "cross", "ch", "c" -> if (sender is Player) {
                val block = sender.getTargetBlock(null, 100).location.toVector()
                block.subtract(target.location.toVector()).normalize().multiply(power)
            } else {
                vectorRelativeYaw(target, 180, power)
            }
            "random", "rand" -> {
                val radians = Math.random().toFloat() * 360
                val rcos = MathUtil.rcos(radians) * power
                val rsin = MathUtil.rsin(radians) * power
                Vector(
                    if (MathUtil.randomBool()) -rcos else rcos,
                    0.0,
                    if (MathUtil.randomBool()) -rsin else rsin
                )
            }
            else -> vectorRelativeYaw(target, 180, power)
        }

        target.velocity = slapVector.setY(power * 0.4)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been slapped by some vengeful spirit", target)
            .toString()
    }
}