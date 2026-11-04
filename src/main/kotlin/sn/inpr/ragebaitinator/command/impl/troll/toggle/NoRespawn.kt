package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PacketEventReceiveSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber

@CommandInfo(
    name = "norespawn",
    description = "Prevents a player from respawning.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoRespawn : BaseToggleTroll(), PacketEventReceiveSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%will now STAY DEAD")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can now respawn")
            .put("player", target.name)
            .toString()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (event.packetType == PacketType.Play.Client.CLIENT_STATUS && players.containsKey(event.user.uuid)) {
            val wrapper = WrapperPlayClientClientStatus(event)
            if (wrapper.action == WrapperPlayClientClientStatus.Action.PERFORM_RESPAWN)
                event.isCancelled = true
        }
    }
}