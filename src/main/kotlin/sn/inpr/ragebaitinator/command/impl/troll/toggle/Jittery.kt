package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerToggleSneakSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "jittery",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.COCOA_BEANS,
    description = "Sneaking is hard after four coffees.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Jittery : BaseToggleTroll(), PlayerToggleSneakSubscriber {
    private val power = param<Double>("Power", "The power of the lurch.", default = 0.6, range = 0.0..5.0)

    override val syntax = withTarget(power)

    private companion object {
        const val SEARCH_RADIUS = 6
        // anything shallower than this isn't really a hole
        const val HOLE_DEPTH = 3
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "power", args[power])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer keep still while sneaking")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has calmed down")
            .put("player", target.name)
            .toString()

    // finds the closest column nearby that's lava, or a drop of at least HOLE_DEPTH blocks, and returns the direction to it
    private fun findHazard(player: Player): Vector? {
        val world = player.world
        val origin = player.location
        val feetY = origin.blockY
        var best: Vector? = null
        var bestDistance = Double.MAX_VALUE

        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dz in -SEARCH_RADIUS..SEARCH_RADIUS) {
                if (dx == 0 && dz == 0)
                    continue

                val x = origin.blockX + dx
                val z = origin.blockZ + dz
                // we won't load a chunk just for this
                if (!world.isChunkLoaded(x shr 4, z shr 4))
                    continue

                val column = (feetY - HOLE_DEPTH..feetY).map { world.getBlockAt(x, it, z).type }
                val lava = Material.LAVA in column
                // the three blocks under the player's feet level are all empty, so stepping there is a fall
                val hole = column.take(HOLE_DEPTH).all { it.isAir }
                if (!lava && !hole)
                    continue

                val direction = Vector(x + 0.5 - origin.x, 0.0, z + 0.5 - origin.z)
                val distance = direction.lengthSquared()
                if (distance < bestDistance) {
                    best = direction
                    bestDistance = distance
                }
            }
        }

        return best?.normalize()
    }

    override fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isCancelled || !event.isSneaking || !hasPlayer(event.player))
            return

        val power = getField<Double>(event.player, "power") ?: 0.6
        // if there's nothing dangerous around, any direction will do
        val direction = findHazard(event.player) ?: MathUtil.random(0.0, 360.0).let { Vector(MathUtil.rcos(it), 0.0, MathUtil.rsin(it)) }
        event.player.velocity = direction.multiply(power).setY(power / 2)
    }
}
