package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.TextureProperty
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.protocol.util.LegacyComponent
import com.github.retrooper.packetevents.wrapper.play.server.*
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import kotlin.math.*
import com.github.retrooper.packetevents.protocol.world.Location as PacketLocation

@CommandInfo(
    name = "fromthefog",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.PLAYER_HEAD,
    description = "You can't see him, but I promise, he can see you.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class FromTheFog : BaseToggleTroll() {
    private val interval = param<Int>("Interval", "The average amount of seconds between each appearance.", default = 90, range = 5..3600)

    override val syntax = withTarget(interval)

    private companion object {
        // the account that actually owns the name "Herobrine" wears a different skin these days
        const val SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc5MzkyMDQ5NzU5NSwKICAicHJvZmlsZUlkIiA6ICI2YTk0NGIzZmU1ZTg0NGYwOTRiODkxN2RlODNkNjZiZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJIZXJvYnJpbmVfTUMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2VkN2VlYzQ0NmNjOWRlNzRjZmQ3NDI5ZGQ2MDZkOTc0ZDc2YmVhODk2MWIyYzgxMDUxYzY3OGQxMDA5NzFhYyIKICAgIH0KICB9Cn0="
        const val SKIN_SIGNATURE = "vAjCjMb7cHBuJTzNcFH6mwXo2EDfG9Y8wF2gAqZJXlac6V/iL4rB7dtBYLPo4A+eMak6PsSUehE6NWSk+YxDSXXkdKAn+lR8g+NNZ5PnBELEGRK8Lo5U3jRdJEM+XHzbrgClA8mHMcEM25bP9MnwMruTpvRuO9FnL86py+gfgDHVpBrUwC+yh6Ui6AA58g8o3Iy/oj7Aq2kF2HWSjrjLUKQuwQ8CeA/DpzECEz0hCHoBxqnVS6UGDASBZFfSIzDICezNXTowoczMTl5AOEm2/wYXhdqLY1GrsgNDIsxN5jKNCodathz1vTLs9/4DVkpZ5hCeL7qcC7nZ9LGT85VJjm34UXg+HobasHRYyHrPICB0+hoSa18uqrspDdw2RbU0XLMsqIqg9GI7+DRmPi3o4jBcAPy9ujYc1OjGhjCsuQE24L9MgeH5H/vlDN66DABxrBC3SuAZfbZaNXLBKV4GkiksaIbwe0O3Chjn4r6kPZhTEnwR/6JOQS2TrVod8bDwu1q0ygtSDkWNzOQsZhwWo5TSx4gdexD6472i/asMcYI1bXZolErr0wXVntvZMusWTraTso3OY5GH4s3nB/wps4auSu4MgiLiTzfImzw57fSHUdNA9aqAXd1JehzUx2BIIf75+tooLAf4jGt1PWLE16vcaW6lSZQixZCtIPRzMwU="
        const val MIN_DISTANCE = 18.0
        const val MAX_DISTANCE = 28.0
        // he doesn't wait around forever, and doesn't let anyone get close either
        const val MAX_LIFETIME_TICKS = 900
        const val MIN_APPROACH_DISTANCE_SQUARED = 8.0 * 8.0
        // how long the player has to be looking at him, in ticks, before he's gone
        const val LOOK_TICKS = 6
        const val LOOK_COSINE = 0.94 // about 20 degrees either side of the crosshair
        const val EYE_HEIGHT = 1.62
        const val CHECK_PERIOD = 2L
        // after standing still for a moment he starts creeping toward the player, far slower than anyone walks
        const val MIN_WALK_DELAY_TICKS = 60
        const val MAX_WALK_DELAY_TICKS = 140
        const val STEP_DISTANCE = 0.08 // per check, so about 0.8 blocks per second
        // he stops here, well out of reach
        const val STOP_DISTANCE = 12.0
        // blocks walked between footsteps
        const val STRIDE = 0.7
        // vanilla sounds carry 16 blocks at a volume of 1, and he's usually further than that
        const val FOOTSTEP_VOLUME = 1.5f
        // he gives up if he's been blocked for this long
        const val MAX_STUCK_TICKS = 120
        // headings to try in order, in degrees off a straight line to the player, when something is in the way
        val detours = listOf(0.0, 45.0, -45.0, 90.0, -90.0).map { Math.toRadians(it) }
    }

    private class Figure(val entityId: Int, val uuid: UUID, name: String, val location: Location) {
        var age = 0
        var lookTicks = 0
        var stuckTicks = 0
        var stride = 0.0
        val walkAfter = MathUtil.random(MIN_WALK_DELAY_TICKS, MAX_WALK_DELAY_TICKS)
        val team = "rb_fog_${name}"
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "interval", args[interval])
        scheduleAppearance(target)
        return placeholdersOr("init", "%prefix% %c2%Something has started watching %c1%%player% %c2%from the fog", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%Whatever was watching %c1%%player% %c2%has gone", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        despawn(player, reason != RemoveReason.LOGOUT)
    }

    private fun scheduleAppearance(player: RbPlayer, delay: Long? = null) {
        val interval = (getField<Int>(player, "interval") ?: 90) * 20.0
        setTask(player, { appear(player) }, delay ?: MathUtil.random(interval * 0.5, interval * 1.5).toLong())
    }

    private fun findSpot(player: RbPlayer): Location? {
        val world = player.world
        val eye = player.eyeLocation
        repeat(12) {
            val flat = LocationUtil.locationRelativeYaw(player, MathUtil.random(100, 261), MathUtil.random(MIN_DISTANCE, MAX_DISTANCE))
            val x = flat.blockX
            val z = flat.blockZ
            // we won't load a chunk just for this
            if (!world.isChunkLoaded(x shr 4, z shr 4))
                return@repeat

            val floor = (player.location.blockY + 6 downTo player.location.blockY - 6).firstOrNull {
                world.getBlockAt(x, it, z).type.isSolid
                    && !world.getBlockAt(x, it + 1, z).type.isSolid && !world.getBlockAt(x, it + 1, z).isLiquid
                    && !world.getBlockAt(x, it + 2, z).type.isSolid && !world.getBlockAt(x, it + 2, z).isLiquid
            } ?: return@repeat

            val spot = Location(world, x + 0.5, floor + 1.0, z + 0.5)
            val head = spot.clone().add(0.0, EYE_HEIGHT, 0.0)
            val toHead = head.toVector().subtract(eye.toVector())
            world.rayTraceBlocks(
                eye,
                toHead.clone().normalize(),
                toHead.length(),
                FluidCollisionMode.NEVER,
                true
            ) ?: return spot
        }

        return null
    }

    private fun lookAt(figure: Location, player: RbPlayer): Pair<Float, Float> {
        val eye = player.eyeLocation
        val dx = eye.x - figure.x
        val dy = eye.y - (figure.y + EYE_HEIGHT)
        val dz = eye.z - figure.z
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val pitch = -Math.toDegrees(atan2(dy, hypot(dx, dz))).toFloat()
        return yaw to pitch
    }

    private fun appear(player: RbPlayer) {
        val spot = if (player.isDead) null else findSpot(player)
        if (spot == null) {
            scheduleAppearance(player, 200)
            return
        }

        val [yaw, pitch] = lookAt(spot, player)
        val name = "hb" + UUID.randomUUID().toString().substring(0, 6)
        val figure = Figure(SpigotReflectionUtil.generateEntityId(), UUID.randomUUID(), name, spot)
        val profile = UserProfile(figure.uuid, name, listOf(TextureProperty("textures", SKIN_VALUE, SKIN_SIGNATURE)))
        val hiddenName = WrapperPlayServerTeams.ScoreBoardTeamInfo(
            LegacyComponent.empty(), LegacyComponent.empty(), LegacyComponent.empty(),
            WrapperPlayServerTeams.NameTagVisibility.NEVER, WrapperPlayServerTeams.CollisionRule.NEVER,
            NamedTextColor.WHITE, WrapperPlayServerTeams.OptionData.NONE
        )

        setField(player, "figure", figure)
        player.sendPacketSilently(
            // the client won't spawn a player it hasn't been told about, but the entry doesn't have to be listed
            WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile, false, 0, GameMode.SURVIVAL, null, null)
            ),
            WrapperPlayServerTeams(figure.team, WrapperPlayServerTeams.TeamMode.CREATE, hiddenName, name),
            WrapperPlayServerSpawnEntity(
                figure.entityId, figure.uuid, EntityTypes.PLAYER,
                PacketLocation(spot.x, spot.y, spot.z, yaw, pitch), yaw, 0, null
            ),
            WrapperPlayServerEntityHeadLook(figure.entityId, yaw)
        )

        setTaskTimer(player, { watch(player) }, CHECK_PERIOD, CHECK_PERIOD)
    }

    private fun isLookedAt(player: RbPlayer, figure: Figure): Boolean {
        val eye = player.eyeLocation
        val head = figure.location.clone().add(0.0, EYE_HEIGHT, 0.0)
        val toHead = head.toVector().subtract(eye.toVector())
        val direction = toHead.clone().normalize()
        return eye.direction.dot(direction) > LOOK_COSINE
            && player.world.rayTraceBlocks(eye, direction, toHead.length(), FluidCollisionMode.NEVER, true) == null
    }

    private fun watch(player: RbPlayer) {
        val figure = getField<Figure>(player, "figure")
        if (figure == null || !player.isOnline) {
            scheduleAppearance(player)
            return
        }

        figure.age += CHECK_PERIOD.toInt()
        figure.lookTicks = if (isLookedAt(player, figure)) figure.lookTicks + CHECK_PERIOD.toInt() else 0

        val gone = player.isDead
            || player.world != figure.location.world
            || figure.age >= MAX_LIFETIME_TICKS
            || figure.lookTicks >= LOOK_TICKS
            || figure.stuckTicks >= MAX_STUCK_TICKS
            || player.location.distanceSquared(figure.location) < MIN_APPROACH_DISTANCE_SQUARED
        if (gone) {
            despawn(player, true)
            scheduleAppearance(player)
            return
        }

        if (figure.age >= figure.walkAfter && walk(player, figure))
            return

        if (figure.age % 10 == 0) {
            val [yaw, pitch] = lookAt(figure.location, player)
            player.sendPacketSilently(
                WrapperPlayServerEntityRotation(figure.entityId, yaw, pitch, true),
                WrapperPlayServerEntityHeadLook(figure.entityId, yaw)
            )
        }
    }

    private fun floorAt(world: World, x: Double, z: Double, fromY: Double): Double? {
        val bx = floor(x).toInt()
        val bz = floor(z).toInt()
        if (!world.isChunkLoaded(bx shr 4, bz shr 4))
            return null

        val top = floor(fromY).toInt() + 1
        return (top downTo top - 3).firstOrNull {
            world.getBlockAt(bx, it - 1, bz).type.isSolid
                && !world.getBlockAt(bx, it, bz).type.isSolid && !world.getBlockAt(bx, it, bz).isLiquid
                && !world.getBlockAt(bx, it + 1, bz).type.isSolid && !world.getBlockAt(bx, it + 1, bz).isLiquid
        }?.toDouble()
    }

    private fun walk(player: RbPlayer, figure: Figure): Boolean {
        val here = figure.location
        val world = here.world ?: return false
        val dx = player.location.x - here.x
        val dz = player.location.z - here.z
        if (hypot(dx, dz) <= STOP_DISTANCE)
            return false

        val heading = atan2(dz, dx)
        for (turn in detours) {
            val x = here.x + cos(heading + turn) * STEP_DISTANCE
            val z = here.z + sin(heading + turn) * STEP_DISTANCE
            val y = floorAt(world, x, z, here.y) ?: continue
            val [yaw, pitch] = lookAt(Location(world, x, y, z), player)
            player.sendPacketSilently(
                WrapperPlayServerEntityRelativeMoveAndRotation(figure.entityId, x - here.x, y - here.y, z - here.z, yaw, pitch, true),
                WrapperPlayServerEntityHeadLook(figure.entityId, yaw)
            )
            here.x = x
            here.y = y
            here.z = z
            figure.stuckTicks = 0

            figure.stride += STEP_DISTANCE
            if (figure.stride >= STRIDE) {
                figure.stride = 0.0
                footstep(player, here)
            }
            return true
        }

        // every heading is blocked
        figure.stuckTicks += CHECK_PERIOD.toInt()
        return true
    }

    private fun footstep(player: RbPlayer, at: Location) {
        val ground = at.world?.getBlockAt(at.blockX, at.blockY - 1, at.blockZ) ?: return
        val sound = ground.blockData.soundGroup.stepSound
        player.playSound(at, sound, SoundCategory.PLAYERS, FOOTSTEP_VOLUME, MathUtil.random(0.7f, 0.9f))
    }

    private fun despawn(player: RbPlayer, notify: Boolean) {
        val figure = takeField<Figure>(player, "figure") ?: return
        if (notify && player.isOnline)
            player.sendPacketSilently(
                WrapperPlayServerDestroyEntities(figure.entityId),
                WrapperPlayServerPlayerInfoRemove(figure.uuid),
                WrapperPlayServerTeams(figure.team, WrapperPlayServerTeams.TeamMode.REMOVE, null as WrapperPlayServerTeams.ScoreBoardTeamInfo?)
            )
    }
}
