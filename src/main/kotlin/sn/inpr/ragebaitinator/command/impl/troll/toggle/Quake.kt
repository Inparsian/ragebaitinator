package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
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
            val signed = if (MathUtil.randomBool()) -rand else rand
            val fovModifier = if (MathUtil.randomBool()) 0f else signed
            sendAbilities(target, fovModifier)
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s field of view is now shaking")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s field of view is now stable")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        runTaskLater({ sendAbilities(player, player.walkSpeed / 2f) }, 1)
    }
}