package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import kotlin.math.pow

@CommandInfo(
    name = "tornado",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.WIND_CHARGE,
    description = "Sends a tornado after a player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Tornado : BaseToggleTroll() {
    private val strength = param<Int>("Strength", "The strength of the tornado (min 1, max 10)", default = 3, range = 1..10)

    override val syntax = withTarget(strength)

    private class Twister(val chase: RbPlayer, var location: Location, val strength: Int) {
        var axis: Int = 0
        val pickupPoint: Location get() = location.clone().add(0.0, strength.toDouble() * 2, 0.0)

        fun intersects(otherLocation: Location): Boolean
                = otherLocation.distanceSquared(pickupPoint) <= ((strength * 1.5).pow(2) + (strength + 2.0).pow(3)) / 2.0

        fun rotate() {
            axis = (axis + 20 - (strength / 1.2).toInt()).mod(360)
        }

        fun move() {
            // no cross-worldsies
            if (chase.world != location.world) {
                val yaw = MathUtil.random(0.0, 360.0)
                location = chase.location.clone().add(MathUtil.rcos(yaw) * 10.0, 2.0, MathUtil.rsin(yaw) * 10.0)
                return
            }

            location.direction = chase.location.toVector().subtract(location.toVector()).normalize()
            location = location.add(location.direction.multiply(0.4))

            val highest = LocationUtil.getHighestBlockBelow(location)
            if (highest != null)
                location.y = highest.y + 1.0
        }

        fun spawnParticles() {
            for (i in 0..(strength * 3)) {
                val yaw = axis - (i * 80)
                val radius = 0.5 * (i + (strength / 2.0))
                val x = radius * MathUtil.rcos(yaw)
                val z = radius * MathUtil.rsin(yaw)
                val x2 = radius * MathUtil.rcos(yaw / 1.5)
                val z2 = radius * MathUtil.rsin(yaw / 1.5)
                val location1 = location.clone().add(x, i.toDouble(), z)
                val location2 = location.clone().add(x2, i.toDouble(), z2)
                chase.world.spawnParticle(Particle.CLOUD, location1, 1, 0.0, 0.0, 0.0, 0.0)
                chase.world.spawnParticle(Particle.CLOUD, location2, 1, 0.0, 0.0, 0.0, 0.0)
            }
        }

        fun pickup() {
            if (intersects(chase.location)) {
                val distance = pickupPoint.distance(chase.location)
                val velocity = pickupPoint.toVector()
                    .add(Vector(MathUtil.rcos(axis) * (strength * 1.5), 0.0, MathUtil.rsin(axis) * (strength * 1.5)))
                    .subtract(chase.location.toVector())
                    .normalize()
                    .multiply((strength * strength) / (distance * 2))
                    .multiply(0.5)
                    .add(chase.velocity)

                // prevent "moved too quickly" errors if their velocity oscillates too quickly
                velocity.x = velocity.x.coerceIn(-5.0, 5.0)
                velocity.y = velocity.y.coerceIn(-5.0, 5.0)
                velocity.z = velocity.z.coerceIn(-5.0, 5.0)

                chase.velocity = velocity
            }
        }

        fun tick() {
            rotate()
            move()
            spawnParticles()
            pickup()
        }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val strength = args[strength]
        val yaw = MathUtil.random(0.0, 360.0)
        setField(target, "twister", Twister(target, target.location.clone().add(
            MathUtil.rcos(yaw) * 10,
            0.0,
            MathUtil.rsin(yaw) * 10,
        ), strength))

        setTaskTimer(target, { getField<Twister>(target, "twister")?.tick() }, 0, 1)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now in Texas", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The tornado chasing %c1%%player% %c2%has dissipated", target)
            .toString()
}