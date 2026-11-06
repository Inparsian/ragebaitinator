package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "spook",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.PRISMARINE_CRYSTALS,
    description = "Plays the elder guardian mob appearance.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ]
)
class Spook : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val packet = WrapperPlayServerChangeGameState(10, 0f)
        PacketEvents.getAPI().playerManager.sendPacket(target, packet)
        return placeholdersOr("init", "%prefix% %c2%Spooked %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}