package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*

@CommandInfo(
    name = "narcolepsy",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.BLACK_BED,
    description = "Zzz...",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Narcolepsy : BaseToggleTroll(), PlayerMoveSubscriber, PlayerInteractSubscriber, EntityDamageSubscriber {
    private companion object {
        // entity metadata indices. these are Entity.DATA_POSE and LivingEntity.SLEEPING_POS_ID, in order of definition
        const val POSE_INDEX = 6
        const val SLEEPING_POS_INDEX = 14

        const val MIN_AWAKE = 200L
        const val MAX_AWAKE = 900L
        const val MIN_ASLEEP = 60L
        const val MAX_ASLEEP = 140L
        const val VIEW_DISTANCE_SQUARED = 128.0 * 128.0
    }

    private val asleep: MutableSet<UUID> = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap())

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        scheduleSleep(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now randomly fall asleep")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now wide awake")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        wake(player)
    }

    private fun scheduleSleep(player: Player, delay: Long = MathUtil.random(MIN_AWAKE, MAX_AWAKE)) {
        addTask(player, { fallAsleep(player) }, delay)
    }

    private fun fallAsleep(player: Player) {
        val ground = player.location.subtract(0.0, 0.1, 0.0).block.type.isSolid
        // lying down midair or on a horse wouldn't look right
        if (!player.isOnline || !ground || player.isDead || player.isInsideVehicle || player.isGliding || player.isSwimming) {
            scheduleSleep(player, 20)
            return
        }

        asleep.add(player.uniqueId)
        broadcast(player, true)
        addTask(player, {
            wake(player)
            scheduleSleep(player)
        }, MathUtil.random(MIN_ASLEEP, MAX_ASLEEP))
    }

    private fun wake(player: Player): Boolean {
        if (!asleep.remove(player.uniqueId))
            return false

        broadcast(player, false)
        return true
    }

    private fun currentPose(player: Player): EntityPose = when {
        player.isGliding -> EntityPose.FALL_FLYING
        player.isSwimming -> EntityPose.SWIMMING
        player.isSneaking -> EntityPose.CROUCHING
        else -> EntityPose.STANDING
    }

    // the player is only asleep as far as clients are concerned, the server never puts them in a bed. a client works
    // out whether an entity is sleeping from the position of its bed, and the entity doesn't need a bed block there
    private fun broadcast(player: Player, sleeping: Boolean) {
        val location = player.location
        val bed = if (sleeping)
            Optional.of(Vector3i(location.blockX, location.blockY, location.blockZ))
        else
            Optional.empty<Vector3i>()
        val metadata = WrapperPlayServerEntityMetadata(player.entityId, listOf(
            EntityData(POSE_INDEX, EntityDataTypes.ENTITY_POSE, if (sleeping) EntityPose.SLEEPING else currentPose(player)),
            EntityData(SLEEPING_POS_INDEX, EntityDataTypes.OPTIONAL_BLOCK_POSITION, bed)
        ))
        val wakeUp = WrapperPlayServerEntityAnimation(
            player.entityId,
            WrapperPlayServerEntityAnimation.EntityAnimationType.WAKE_UP
        )

        val viewers = player.world.players.filter {
            it == player || (it.canSee(player) && it.location.distanceSquared(location) <= VIEW_DISTANCE_SQUARED)
        }
        for (viewer in viewers) {
            PacketEvents.getAPI().playerManager.sendPacket(viewer, metadata)
            // clearing the bed position is enough to make an entity stand up, but this is how the game itself does it
            if (!sleeping)
                PacketEvents.getAPI().playerManager.sendPacket(viewer, wakeUp)
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (event is PlayerTeleportEvent || event.player.uniqueId !in asleep)
            return

        val from = event.from
        val to = event.to ?: return
        // they can still turn their head, and fall
        event.cancelIf(from.x != to.x || from.z != to.z || to.y > from.y)
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) = event.cancelIf(event.player.uniqueId in asleep)

    // getting hurt wakes anyone up
    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (!event.isCancelled && wake(player))
            scheduleSleep(player)
    }
}
