package sn.inpr.ragebaitinator.command.impl.toggletroll

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
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.refreshFor

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
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders {
        target.others.filter { target.canSee(it) }.refreshFor(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now sees everyone else as themselves", target)
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can tell everyone apart again", target)

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        if (player.isOnline && reason != RemoveReason.LOGOUT)
            player.others.filter { player.canSee(it) }.refreshFor(player)
    }

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
