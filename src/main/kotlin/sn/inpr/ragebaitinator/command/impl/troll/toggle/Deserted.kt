package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "deserted",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.DEAD_BUSH,
    description = "Where did everyone go?",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Deserted : BaseToggleTroll(), PacketEventSendSubscriber, AsyncPlayerChatSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val others = target.others
        if (others.isNotEmpty()) {
            target.sendPacket(WrapperPlayServerDestroyEntities(*others.mapNotNull { it.bukkit?.entityId }.toIntArray()))
            target.sendPacket(WrapperPlayServerPlayerInfoRemove(others.map { it.uuid }))
        }

        return placeholdersOr("init", "%prefix% %c2%Everyone has vanished from %c1%%player%%c2%'s world")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%Everyone is back in %c1%%player%%c2%'s world")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        if (reason == "logout" || !player.isOnline)
            return

        for (other in player.others) {
            player.hidePlayer(other)
            player.showPlayer(other)
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        if (!hasPlayer(uuid))
            return

        val ownId = Bukkit.getPlayer(uuid)?.entityId
        when (event.packetType) {
            PacketType.Play.Server.SPAWN_PLAYER -> // <=1.20.1
                if (WrapperPlayServerSpawnPlayer(event).entityId != ownId) event.isCancelled = true
            PacketType.Play.Server.SPAWN_ENTITY -> { // >=1.20.2
                val wrapper = WrapperPlayServerSpawnEntity(event)
                if (wrapper.entityType == EntityTypes.PLAYER && wrapper.entityId != ownId)
                    event.isCancelled = true
            }
            // keep every tab list entry but the player's own from being added or updated
            PacketType.Play.Server.PLAYER_INFO_UPDATE -> {
                val wrapper = WrapperPlayServerPlayerInfoUpdate(event)
                val own = wrapper.entries.filter { it.profileId == uuid }
                if (own.isEmpty())
                    event.isCancelled = true
                else if (own.size != wrapper.entries.size) {
                    wrapper.entries = own
                    event.markForReEncode(true)
                }
            }
        }
    }

    // the chat of every other player never reaches a deserted player. their own still does
    override fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!event.isCancelled)
            event.recipients.removeIf { it != event.player && hasPlayer(it) }
    }
}
