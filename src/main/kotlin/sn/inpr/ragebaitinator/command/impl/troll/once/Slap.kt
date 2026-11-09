package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Aliased
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
    // what they can also be called is what they're shortened to
    enum class Direction(override val aliases: List<String> = emptyList()) : Aliased {
        NORTH(listOf("n")), NORTHEAST(listOf("ne")),
        EAST(listOf("e")), SOUTHEAST(listOf("se")),
        SOUTH(listOf("s")), SOUTHWEST(listOf("sw")),
        WEST(listOf("w")), NORTHWEST(listOf("nw")),
        BACKWARD(listOf("b")), FORWARD(listOf("f")),
        LEFT(listOf("l")), RIGHT(listOf("r")),
        CROSSHAIR(listOf("cross", "ch", "c")), SENDER(listOf("me")), RANDOM(listOf("rand"));
    }

    private val direction = param<Direction>(
        "Direction",
        "The direction to slap the player (cardinal, ordinal, backward/forward/left/right, crosshair, sender, random)",
        default = Direction.BACKWARD,
    )

    override val syntax = withTarget(power, direction)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val power: Double = args[power]
        val direction = args[direction]
        val slapVector: Vector = when (direction) {
            Direction.NORTH -> Vector(0.0, 0.0, -power)
            Direction.NORTHEAST -> Vector(power, 0.0, -power)
            Direction.EAST -> Vector(power, 0.0, 0.0)
            Direction.SOUTHEAST -> Vector(power, 0.0, power)
            Direction.SOUTH -> Vector(0.0, 0.0, power)
            Direction.SOUTHWEST -> Vector(-power, 0.0, power)
            Direction.WEST -> Vector(-power, 0.0, 0.0)
            Direction.NORTHWEST -> Vector(-power, 0.0, -power)
            Direction.BACKWARD -> vectorRelativeYaw(target, 180, power)
            Direction.FORWARD -> vectorRelativeYaw(target, 0, power)
            Direction.LEFT -> vectorRelativeYaw(target, -90, power)
            Direction.RIGHT -> vectorRelativeYaw(target, 90, power)
            Direction.SENDER -> if (sender is Player) vectorRelativeYaw(sender, 0, power) else vectorRelativeYaw(target, 180, power)
            Direction.CROSSHAIR -> if (sender is Player) {
                val block = sender.getTargetBlock(null, 100).location.toVector()
                block.subtract(target.location.toVector()).normalize().multiply(power)
            } else {
                vectorRelativeYaw(target, 180, power)
            }
            Direction.RANDOM -> {
                val radians = Math.random().toFloat() * 360
                val rcos = MathUtil.rcos(radians) * power
                val rsin = MathUtil.rsin(radians) * power
                Vector(
                    if (MathUtil.randomBool()) -rcos else rcos,
                    0.0,
                    if (MathUtil.randomBool()) -rsin else rsin
                )
            }
        }

        target.velocity = slapVector.setY(power * 0.4)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been slapped by some vengeful spirit", target)
            .toString()
    }
}