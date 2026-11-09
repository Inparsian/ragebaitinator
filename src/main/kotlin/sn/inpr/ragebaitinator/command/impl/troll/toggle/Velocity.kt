package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import java.util.*

@CommandInfo(
    name = "velocity",
    category = CommandInfo.Category.COMBAT,
    icon = Material.SPECTRAL_ARROW,
    description = "Magnifies a player's velocity when they take damage.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Multiplier",
            description = "The multiplier to apply to the player's velocity",
            default = "2"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Velocity : BaseToggleTroll(), PacketEventSendSubscriber {
    private val multipliers = mutableMapOf<UUID, Double>()

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val multiplier = args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0..5.0) ?: 2.0
        multipliers[target.uuid] = multiplier
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has built-in %c1%%multiplier%x %c2%velocity hacks")
            .put("player", target.name)
            .put("multiplier", multiplier.toString())
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s velocity is no longer being magnified")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        multipliers.remove(player.uuid)
    }

    fun markDamaged(uuid: UUID) {
        addFlag(uuid, "damaged")
        // in case a player somehow doesn't get unmarked
        addTask(uuid, { removeFlag(uuid, "damaged") }, 20)
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        if (hasPlayer(uuid)) {
            when (event.packetType) {
                // velocity is usually dealt after damage
                PacketType.Play.Server.DAMAGE_EVENT -> markDamaged(uuid)
                PacketType.Play.Server.ENTITY_ANIMATION -> { // pre-1.19.4
                    val wrapper = WrapperPlayServerEntityAnimation(event)
                    if (wrapper.type == WrapperPlayServerEntityAnimation.EntityAnimationType.HURT)
                        markDamaged(uuid)
                }
                PacketType.Play.Server.ENTITY_VELOCITY -> if (removeFlag(uuid, "damaged")) {
                    val multiplier = multipliers[uuid] ?: 2.0 // saftey
                    val wrapper = WrapperPlayServerEntityVelocity(event)
                    wrapper.velocity = wrapper.velocity.multiply(multiplier)
                    removeTask(uuid)
                }
            }
        }
    }
}