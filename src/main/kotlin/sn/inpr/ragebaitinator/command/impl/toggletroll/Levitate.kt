package sn.inpr.ragebaitinator.command.impl.toggletroll

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "levitate",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.FEATHER,
    description = "Ascends a player to the heavens.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Levitate : BaseToggleTroll(), PacketEventSendSubscriber {
    private val velocity = param<Double>("Velocity", "How fast the player should ascend.", default = 0.15, range = 0.01..5.0)

    override val syntax = withTarget(velocity)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setTaskTimer(target, { target.velocity = Vector(target.velocity.x, args[velocity], target.velocity.z) }, 0, 1)

        // vanilla dumbpidity
        setTaskTimer(target, "ihatevanilla", {
            if (!target.allowFlight) {
                addFlag(target, "cancelNextAllowFlight")
                target.allowFlight = true
                runTaskLater({ target.allowFlight = false }, 1)
            }
        }, 10, 10)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now ascending to the heavens", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now falling back towards the earth", target)
            .toString()

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        if (event.packetType == PacketType.Play.Server.PLAYER_ABILITIES && hasPlayer(uuid)) {
            val abilities = WrapperPlayServerPlayerAbilities(event)
            if (abilities.isFlightAllowed && hasFlag(uuid, "cancelNextAllowFlight")) {
                event.isCancelled = true
                removeFlag(uuid, "cancelNextAllowFlight")
            }
        }
    }
}
