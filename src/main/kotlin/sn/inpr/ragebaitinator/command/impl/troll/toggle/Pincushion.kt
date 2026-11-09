package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.sendPacket
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "pincushion",
    category = CommandInfo.Category.COMBAT,
    icon = Material.SWEET_BERRIES,
    description = "Looks like they lost a fight with a skeleton. Sticks fake arrows or bee stingers in a player, which everyone can see.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Pincushion : BaseToggleTroll(), PacketEventSendSubscriber {
    @Suppress("unused")
    enum class Kind { ARROWS, STINGERS, BOTH }

    private val type = param<Kind>("Type", "What is stuck in them: arrows, stingers or both.", default = Kind.ARROWS)
    private val count = param<Int>("Count", "How many of each (min 1, max 100).", default = 20, range = 1..100)

    override val syntax = withTarget(type, count)

    private companion object {
        const val ARROW_COUNT_INDEX = 12
        const val STINGER_COUNT_INDEX = 13
    }

    private data class Stuck(val arrows: Int, val stingers: Int)
    private val stuck = ConcurrentHashMap<Int, Stuck>()

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val type = args[type]
        val count = args[count]
        stuck[target.entityId] = Stuck(
            if (type == Kind.STINGERS) 0 else count,
            if (type != Kind.ARROWS) count else 0
        )
        broadcast(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now full of holes", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been patched up", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        stuck.remove(player.entityId)
        if (player.isOnline && reason != "logout")
            broadcast(player)
    }

    private fun metadata(stuck: Stuck) = listOf<EntityData<*>>(
        EntityData(ARROW_COUNT_INDEX, EntityDataTypes.INT, stuck.arrows),
        EntityData(STINGER_COUNT_INDEX, EntityDataTypes.INT, stuck.stingers)
    )

    private fun broadcast(player: RbPlayer) {
        val packet = WrapperPlayServerEntityMetadata(
            player.entityId,
            metadata(stuck[player.entityId] ?: Stuck(0, 0))
        )
        player.viewers().sendPacket(packet)
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (stuck.isEmpty() || event.packetType != PacketType.Play.Server.ENTITY_METADATA)
            return

        val wrapper = WrapperPlayServerEntityMetadata(event)
        val state = stuck[wrapper.entityId] ?: return
        val data = wrapper.entityMetadata.filter { it.index != ARROW_COUNT_INDEX && it.index != STINGER_COUNT_INDEX }
        wrapper.entityMetadata = data + metadata(state)
    }
}
