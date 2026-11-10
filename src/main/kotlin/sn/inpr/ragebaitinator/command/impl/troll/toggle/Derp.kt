package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "derp",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.SKELETON_SKULL,
    description = "Something is off about their neck. Makes a player's head spasm every so often.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Derp : BaseToggleTroll() {
    private val interval = param<Double>("Interval", "The amount of seconds between each spasm (min 0.1, max 10).", default = 2.0, range = 0.1..10.0)

    override val syntax = withTarget(interval)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val seconds = args[interval]
        val period = (seconds * 20).toLong()
        setTaskTimer(target, { spasm(target) }, period, period)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s head is now spasming", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s head has stopped spasming", target)
            .toString()

    private fun spasm(player: RbPlayer) {
        val yaw = MathUtil.random(0.0, 360.0)
        val pitch = MathUtil.random(-90.0, 90.0)
        // only their view moves, so it doesn't interfere with anything they're in the middle of
        player.sendPacket(WrapperPlayServerPlayerRotation(yaw.toFloat(), pitch.toFloat()))
    }
}
