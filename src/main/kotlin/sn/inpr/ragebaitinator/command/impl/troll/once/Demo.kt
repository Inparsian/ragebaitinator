package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "demo",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.WRITTEN_BOOK,
    description = "WTF I thought my account was genuine??!1",
)
class Demo : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.sendPacket(WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.DEMO_EVENT, 0f))
        return placeholdersOr("init", "%prefix% %c2%Sent demo message to %c1%%player%", target)
            .toString()
    }
}