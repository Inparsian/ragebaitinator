package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Mob
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
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
class MobMask : BaseToggleTroll(), PacketEventSendSubscriber {
    private val type = param<String>("Type", "The mob that every other mob looks like (like creeper or pig).", default = "creeper")

    override val syntax = withTarget(type)

    private companion object {
        const val LAST_SHARED_INDEX = 15

        // a lookup by name wants the namespace in some versions of PacketEvents, and not in others
        fun resolve(name: String): EntityType? {
            val bare = name.lowercase().removePrefix("minecraft:")
            return listOf("minecraft:$bare", bare).firstNotNullOfOrNull { runCatching { EntityTypes.getByName(it) }.getOrNull() }
        }

        fun isMob(type: EntityType): Boolean = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_INSENTIENT)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val name = args[type]
        val mask = resolve(name)?.takeIf { isMob(it) }
        if (mask == null) {
            removePlayer(target, RemoveReason.FAILED)
            return "&c'$name' is not a mob."
        }

        setField(target, "mask", mask)
        setField(target, "masked", ConcurrentHashMap.newKeySet<Int>())
        resend(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now sees every mob as a %c1%%mask%", target)
            .put("mask", name.lowercase().removePrefix("minecraft:"))
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can tell the mobs apart again", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        if (player.isOnline && reason != RemoveReason.LOGOUT)
            resend(player)
    }

    // hiding and showing an entity makes the server send it over again, going through the masking this time
    private fun resend(player: RbPlayer) {
        val body = player.bukkit ?: return
        val plugin = Main.instance()
        for (mob in player.nearbyEntities(80.0).filterIsInstance<Mob>()) {
            body.hideEntity(plugin, mob)
            body.showEntity(plugin, mob)
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
