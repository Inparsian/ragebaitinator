package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.PacketEvents
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState

@CommandInfo(
    name = "demo",
    description = "WTF I thought my account was genuine??!1",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class Demo : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val packet = WrapperPlayServerChangeGameState(5, 0f)
        PacketEvents.getAPI().playerManager.sendPacket(target, packet)
        return placeholdersOr("init", "%prefix% %c2%Sent demo message to %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}