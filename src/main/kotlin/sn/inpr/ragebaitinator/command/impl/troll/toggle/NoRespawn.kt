package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PacketEventReceiveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "norespawn",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.RESPAWN_ANCHOR,
    description = "Prevents a player from respawning.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoRespawn : BaseToggleTroll(), PacketEventReceiveSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now STAY DEAD")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now respawn")
            .put("player", target.name)
            .toString()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType == PacketType.Play.Client.CLIENT_STATUS && event.user.uuid?.let { hasPlayer(it) } == true) {
            val wrapper = WrapperPlayClientClientStatus(event)
            if (wrapper.action == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN)
                event.isCancelled = true
        }
    }
}