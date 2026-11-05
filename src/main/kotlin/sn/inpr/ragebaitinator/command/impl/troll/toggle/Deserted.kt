package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber

@CommandInfo(
    name = "deserted",
    description = "Where did everyone go?",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Deserted : BaseToggleTroll(), PacketEventSendSubscriber, AsyncPlayerChatSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val others = Bukkit.getOnlinePlayers().filter { it != target }
        if (others.isNotEmpty()) {
            val api = PacketEvents.getAPI().playerManager
            api.sendPacket(target, WrapperPlayServerDestroyEntities(*others.map { it.entityId }.toIntArray()))
            api.sendPacket(target, WrapperPlayServerPlayerInfoRemove(others.map { it.uniqueId }))
        }

        return placeholdersOr("init", "%prefix% %c2%Everyone has vanished from %c1%%player%'s %c2%world")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%Everyone is back in %c1%%player%'s %c2%world")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        if (reason == "logout" || !player.isOnline)
            return

        val plugin = Main.instance()
        for (other in Bukkit.getOnlinePlayers().filter { it != player }) {
            player.hidePlayer(plugin, other)
            player.showPlayer(plugin, other)
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
