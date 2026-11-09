package sn.inpr.ragebaitinator.utility

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil.rcos
import sn.inpr.ragebaitinator.utility.MathUtil.rsin
import kotlin.math.floor

object LocationUtil {
    // The vertical height limit was extended 64 blocks in both directions starting in 1.18
    val downHeightLimit = if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_18)) -64 else 0
    val upHeightLimit = if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_18)) 319 else 255
    // The max altitude a player can be teleported to. Any higher than this will cause them to be rubber-banded back
    const val MAX_PLAYER_ALTITUDE = 20_000_000.0

    fun vectorRelativeYaw(player: RbPlayer, radians: Int, distance: Double): Vector
        = vectorRelativeYaw(player.location.yaw, radians, distance)

    fun vectorRelativeYaw(entity: Entity, radians: Int, distance: Double): Vector
        = vectorRelativeYaw(entity.location.yaw, radians, distance)

    private fun vectorRelativeYaw(baseYaw: Float, radians: Int, distance: Double): Vector {
        val yaw: Double = baseYaw.toDouble() + radians
        return Vector(-rsin(yaw) * distance, 0.0, rcos(yaw) * distance)
    }

    fun locationRelativeYaw(player: RbPlayer, radians: Int, distance: Double): Location
        = vectorRelativeYaw(player, radians, distance).toLocation(player.world).add(player.location)

    fun locationRelativeYaw(entity: Entity, radians: Int, distance: Double): Location
        = vectorRelativeYaw(entity, radians, distance).toLocation(entity.world).add(entity.location)

    fun chunkify(vector: Vector): Vector = Vector(
        vector.x.toInt() shr 4,
        vector.y.toInt(),
        vector.z.toInt() shr 4
    )

    fun chunkify(location: Location): Vector = chunkify(location.toVector())

    fun blockify(location: Location, pointFive: Boolean): Location {
        val factor = if (pointFive) 0.5 else 0.0
        return Location(
            location.world,
            location.blockX + factor, location.blockY + factor, location.blockZ + factor,
            location.yaw, location.pitch
        )
    }

    fun getHighestBlockBelow(location: Location): Location? {
        val world = location.world
        val x = location.blockX
        val z = location.blockZ

        // The chunk must be loaded, if not, then we won't load it *just* for this
        if (!world!!.isChunkLoaded(x shr 4, z shr 4))
            return null

        for (y in location.blockY downTo 0) {
            if (!world.getBlockAt(x, y, z).isEmpty)
                return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        }

        return null
    }

    // thx nik
    private fun chunkRadiusMin(x: Double, radius: Double) = floor((x - radius) / 16.0).toInt()
    private fun chunkRadiusMax(x: Double, radius: Double) = floor((x + radius) / 16.0).toInt()
    fun getEntitiesWithinRadius(location: Location, radius: Double): List<Entity> {
        if (location.world == null) return emptyList()

        val entities: MutableList<Entity> = mutableListOf()
        for (x in chunkRadiusMin(location.x, radius)..chunkRadiusMax(location.x, radius)) {
            for (z in chunkRadiusMin(location.z, radius)..chunkRadiusMax(location.z, radius)) {
                if (!location.world!!.isChunkLoaded(x, z)) continue

                for (entity in location.world!!.getChunkAt(x, z).entities)
                    if (entity != null && entity.location.distanceSquared(location) <= radius * radius)
                        entities.add(entity)
            }
        }

        return entities.toList()
    }

    fun createSafeExplosion(location: Location, power: Float) {
        location.world?.createExplosion(location.x, location.y, location.z, power, false, false)
    }

    fun isFinite(vector: Vector): Boolean = vector.x.isFinite() && vector.y.isFinite() && vector.z.isFinite()

    private fun msqrt(x: Int, y: Int) = (x - y) * (x - y)
    fun cylinder(center: Vector, radius: Double, height: Int): Set<Vector> {
        val blocks: MutableSet<Vector> = mutableSetOf()

        for (x in center.blockX - radius.toInt()..center.blockX + radius.toInt()) {
            for (z in center.blockZ - radius.toInt()..center.blockZ + radius.toInt()) {
                for (y in center.blockY..<center.blockY + height) {
                    val dist = msqrt(center.blockX, x) + msqrt(center.blockZ, z)
                    if (dist < radius * radius) {
                        blocks.add(Vector(x, y, z))
                    }
                }
            }
        }

        return blocks.toSet()
    }

    fun sphere(center: Vector, radius: Double): Set<Vector> {
        val blocks: MutableSet<Vector> = mutableSetOf()

        for (x in center.blockX - radius.toInt()..center.blockX + radius.toInt()) {
            for (z in center.blockZ - radius.toInt()..center.blockZ + radius.toInt()) {
                for (y in center.blockY - radius.toInt()..center.blockY + radius.toInt()) {
                    val dist = msqrt(center.blockX, x) + msqrt(center.blockZ, z) + msqrt(center.blockY, y)
                    if (dist < radius * radius) {
                        blocks.add(Vector(x, y, z))
                    }
                }
            }
        }

        return blocks.toSet()
    }
}