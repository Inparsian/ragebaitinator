package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "spook",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.PRISMARINE_CRYSTALS,
    description = "Plays the elder guardian mob appearance.",
)
class Spook : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.sendPacket(WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.PLAY_ELDER_GUARDIAN_MOB_APPEARANCE, 0f))
        return placeholdersOr("init", "%prefix% %c2%Spooked %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}