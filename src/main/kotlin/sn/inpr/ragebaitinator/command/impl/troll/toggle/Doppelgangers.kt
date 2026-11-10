package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "doppelgangers",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.ARMOR_STAND,
    description = "WHY does everyone look like me?",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Doppelgangers : BaseToggleTroll(), PacketEventSendSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        resend(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now sees everyone else as themselves", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can tell everyone apart again", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        if (player.isOnline && reason != RemoveReason.LOGOUT)
            resend(player)
    }

    private fun resend(player: RbPlayer) {
        for (other in player.others) {
            if (!player.canSee(other))
                continue

            player.hidePlayer(other)
            player.showPlayer(other)
        }
    }

    private fun RbPlayer.canSee(other: RbPlayer): Boolean = bukkit?.let { viewer -> other.bukkit?.let { viewer.canSee(it) } } == true

    override fun onPacketSend(event: PacketSendEvent) {
        if (players.isEmpty() || event.packetType != PacketType.Play.Server.PLAYER_INFO_UPDATE)
            return

        val viewer = event.user.uuid ?: return
        if (!hasPlayer(viewer))
            return

        val wrapper = WrapperPlayServerPlayerInfoUpdate(event)
        if (WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER !in wrapper.actions)
            return

        val skin = event.user.profile.textureProperties
        for (entry in wrapper.entries)
            if (entry.gameProfile.uuid != viewer)
                entry.gameProfile.textureProperties = ArrayList(skin)
    }
}
