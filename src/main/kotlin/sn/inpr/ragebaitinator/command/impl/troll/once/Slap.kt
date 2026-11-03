package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.LocationUtil.vectorRelativeYaw
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil.setVelocity

@CommandInfo(
    name = "slap",
    description = "Slaps the player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Power",
            description = "The power of the slap.",
            default = "2"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.STRING,
            purpose = "Direction",
            description = "The direction to slap the player (cardinal, ordinal, backward/forward/left/right, crosshair, sender, random)",
            strictCompletion = true,
            completions = [
                "north", "northeast",
                "east", "southeast",
                "south", "southwest",
                "west", "northwest",
                "backward", "forward",
                "left", "right",
                "crosshair", "sender", "random"
            ],
            completionAliases = [
                "n", "ne", "e", "se", "s", "sw", "w", "nw",
                "b", "f", "l", "r", "me", "cross", "ch", "c",
                "rand"
            ],
            default = "backward"
        )
    ]
)
class Slap : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val power: Double = (args.getOrNull(2)?.toDoubleOrNull() ?: 2.0).coerceIn(0.0, 5.0)
        val direction = args.getOrNull(3) ?: "backward"
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
            "sender", "me" -> vectorRelativeYaw(sender as? Player ?: target, if (sender is Player) 0 else 180, power)
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

        setVelocity(target, slapVector.setY(power * 0.4))

        return "&c${target.name} &fhas been slapped by some vengeful spirit"
    }
}