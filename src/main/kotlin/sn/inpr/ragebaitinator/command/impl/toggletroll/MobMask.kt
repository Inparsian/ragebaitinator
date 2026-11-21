package sn.inpr.ragebaitinator.command.impl.toggletroll

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Mob
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.command.def.common.BaseMobTroll
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "mobmask",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.ZOMBIE_HEAD,
    description = "Everything is a creeper! Every mob looks like another kind of mob to a player. Nobody else sees it.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class MobMask : BaseMobTroll(), PacketEventSendSubscriber {
    private val type = mobParam("Type", "The mob that every other mob looks like.")

    override val syntax = withTarget(type)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders {
        val name = sanitize(args[type].lowercase())
        val mask = resolve(name)?.takeIf { isMob(it) }
        if (mask == null)
            return errPlaceholdersOr("invalidMob", "%prefix% &c'%name%' is not a valid mob.", target)
                .put("name", name)

        setField(target, "mask", mask)
        setField(target, "masked", ConcurrentHashMap.newKeySet<Int>())
        resend(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now sees every mob as a %c1%%mask%", target)
            .put("mask", name)
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can tell the mobs apart again", target)

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        if (player.isOnline && reason != RemoveReason.LOGOUT)
            resend(player)
    }

    // hiding and showing an entity makes the server send it over again, going through the masking this time
    private fun resend(player: RbPlayer) {
        for (mob in player.nearbyEntities(80.0).filterIsInstance<Mob>()) {
            player.hideEntity(mob)
            player.showEntity(mob)
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (players.isEmpty())
            return

        val packetType = event.packetType
        if (packetType != PacketType.Play.Server.SPAWN_ENTITY &&
            packetType != PacketType.Play.Server.ENTITY_METADATA &&
            packetType != PacketType.Play.Server.DESTROY_ENTITIES)
            return

        val viewer = event.user.uuid?.let { RbPlayer(it) } ?: return
        val mask = getField<EntityType>(viewer, "mask") ?: return
        val masked = getField<MutableSet<Int>>(viewer, "masked") ?: return

        when (packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val wrapper = WrapperPlayServerSpawnEntity(event)
                if (wrapper.entityType == mask || !isMob(wrapper.entityType))
                    return

                masked.add(wrapper.entityId)
                wrapper.entityType = mask
            }
            PacketType.Play.Server.ENTITY_METADATA -> {
                val wrapper = WrapperPlayServerEntityMetadata(event)
                if (wrapper.entityId in masked)
                    wrapper.entityMetadata = wrapper.entityMetadata.filter { it.index <= LAST_SHARED_INDEX }
            }
            PacketType.Play.Server.DESTROY_ENTITIES -> {
                // ids get reused, and an entity that is gone is not masked anymore
                WrapperPlayServerDestroyEntities(event).entityIds.forEach { masked.remove(it) }
            }
        }
    }
}
