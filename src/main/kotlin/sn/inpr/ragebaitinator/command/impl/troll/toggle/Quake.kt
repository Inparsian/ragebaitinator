package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "quake",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.SPYGLASS,
    description = "Constantly changes the player's FOV.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Quake : BaseToggleTroll() {
    fun abilities(target: RbPlayer, fovModifier: Float) = WrapperPlayServerPlayerAbilities(
        target.isInvulnerable,
        target.isFlying,
        target.allowFlight,
        target.gameMode == GameMode.CREATIVE,
        target.flySpeed / 2f,
        fovModifier
    )

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setTaskTimer(target, {
            val rand = MathUtil.random(0.8f, 1.0f)
            val signed = if (MathUtil.randomBool()) -rand else rand
            val fovModifier = if (MathUtil.randomBool()) 0f else signed
            target.sendPacket(abilities(target, fovModifier))
        }, 0, 1)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s field of view is now shaking", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s field of view is now stable", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        runTaskLater({ player.sendPacket(abilities(player, player.walkSpeed / 2f)) }, 1)
    }
}