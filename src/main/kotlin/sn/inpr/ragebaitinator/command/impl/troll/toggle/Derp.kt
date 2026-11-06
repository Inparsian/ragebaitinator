package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "derp",
    description = "Something is off about their neck. Makes a player's head spasm every so often.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Interval",
            description = "The amount of seconds between each spasm (min 0.1, max 10).",
            default = "2"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Derp : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val seconds = (args.getOrNull(2)?.toDoubleOrNull() ?: 2.0).coerceIn(0.1, 10.0)
        val period = (seconds * 20).toLong()
        addTaskTimer(target, { spasm(target) }, period, period)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s head is now spasming")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s head has stopped spasming")
            .put("player", target.name)
            .toString()

    private fun spasm(player: Player) {
        val yaw = MathUtil.random(0.0, 360.0)
        val pitch = MathUtil.random(-90.0, 90.0)
        // only their view moves, so it doesn't interfere with anything they're in the middle of
        PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerPlayerRotation(yaw.toFloat(), pitch.toFloat()))
    }
}
