package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "credits",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.MUSIC_DISC_13,
    description = "Roll the credits. Plays the end poem for a player, whether they've earned it or not.",
)
class Credits : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        // the server never marks them as having won, so when the credits end the game just carries on
        target.sendPacket(WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.WIN_GAME, 1f))
        return placeholdersOr("init", "%prefix% %c2%Rolled the credits for %c1%%player%", target)
            .toString()
    }
}
