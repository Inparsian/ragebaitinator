package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "pincushion",
    category = CommandInfo.Category.COMBAT,
    icon = Material.SWEET_BERRIES,
    description = "Looks like they lost a fight with a skeleton. Sticks fake arrows or bee stingers in a player, which everyone can see.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.STRING,
            purpose = "Type",
            description = "What is stuck in them: arrows, stingers or both.",
            strictCompletion = true,
            completions = ["arrows", "stingers", "both"],
            default = "arrows"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Count",
            description = "How many of each (min 1, max 100).",
            default = "20"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Pincushion : BaseToggleTroll(), PacketEventSendSubscriber {
    private companion object {
        const val ARROW_COUNT_INDEX = 12
        const val STINGER_COUNT_INDEX = 13
        const val VIEW_DISTANCE_SQUARED = 128.0 * 128.0
    }

    private data class Stuck(val arrows: Int, val stingers: Int)
    private val stuck = ConcurrentHashMap<Int, Stuck>()

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val type = args.getOrNull(2)?.lowercase() ?: "arrows"
        val count = (args.getOrNull(3)?.toIntOrNull() ?: 20).coerceIn(1, 100)
        stuck[target.entityId] = Stuck(
            if (type == "stingers") 0 else count,
            if (type == "stingers" || type == "both") count else 0
        )
        broadcast(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now full of holes")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been patched up")
            .put("player", target.name)
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
        val location = player.location
        for (viewer in player.world.players) {
            if (viewer.uniqueId == player.uuid || (player.isVisibleTo(viewer) && viewer.location.distanceSquared(location) <= VIEW_DISTANCE_SQUARED))
                PacketEvents.getAPI().playerManager.sendPacket(viewer, packet)
        }
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
