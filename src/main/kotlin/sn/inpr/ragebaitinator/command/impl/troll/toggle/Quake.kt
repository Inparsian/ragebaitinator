package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "quake",
    description = "Constantly changes the player's FOV.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Quake : BaseToggleTroll() {
    fun sendAbilities(target: Player, fovModifier: Float) {
        val abilities = WrapperPlayServerPlayerAbilities(
            target.isInvulnerable,
            target.isFlying,
            target.allowFlight,
            target.gameMode == GameMode.CREATIVE,
            target.flySpeed / 2f,
            fovModifier,
        )

        PacketEvents.getAPI().playerManager.sendPacket(target, abilities)
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        addTaskTimer(target, {
            val rand = MathUtil.random(0.8f, 1.0f)
            val fovModifier = if (MathUtil.randomBool())
                0f
            else if (MathUtil.randomBool())
                -rand
            else
                rand

            sendAbilities(target, fovModifier)
        }, 0, 1, true)
        return "&c${target.name}&f's field of view is now shaking"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name}&f's field of view is now stable"

    override fun onRemovePlayer(player: Player, reason: String) {
        runTaskLater({ sendAbilities(player, player.walkSpeed / 2f) }, 1)
    }
}