package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.IronGolem
import org.bukkit.entity.LivingEntity
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "exorcist",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.BELL,
    description = "Causes entities around a player to spaz out.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Exorcist : BaseToggleTroll() {
    private val local = param<Boolean>("Local", "Whether this should only be visible to the player.", default = true)

    override val syntax = withTarget(local)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[local]) addFlag(target, "local")

        setTaskRandomTimer(target, {
            for (entity in target.nearbyEntities(64.0)) {
                if (entity !is LivingEntity || entity is ArmorStand)
                    continue

                val status: Int? = when(entity) {
                    is IronGolem -> 4
                    else -> null
                }

                val animation = if (status != null)
                    WrapperPlayServerEntityStatus(entity.entityId, status)
                else
                    WrapperPlayServerEntityAnimation(entity.entityId, EntityAnimationType.SWING_MAIN_ARM)
                val look = WrapperPlayServerEntityRotation(entity.entityId, MathUtil.random(0f, 255f), MathUtil.random(0f, 255f), entity.isOnGround)
                val head = WrapperPlayServerEntityHeadLook(entity.entityId, MathUtil.random(0f, 255f))

                if (hasFlag(target, "local")) {
                    target.sendPacket(animation, look, head)
                } else for (player in target.world.players) {
                    // This entity should be within 64 blocks from *this* player
                    if (entity.location.distanceSquared(player.location) > 64.0 * 64.0)
                        continue
                    player.rb.sendPacket(animation, look, head)
                }
            }
        }, 0, 1, 3)

        return placeholdersOr("init", "%prefix% %c2%Entities around %c1%%player% %c2%are now spazzing out", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        return placeholdersOr("deinit", "%prefix% %c2%Entities around %c1%%player% %c2%are no longer spazzing out", target)
            .toString()
    }
}