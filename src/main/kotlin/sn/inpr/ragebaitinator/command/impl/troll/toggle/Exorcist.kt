package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.IronGolem
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "exorcist",
    description = "Causes entities around a player to spaz out.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Local",
            description = "Whether this should only be visible to the player.",
            default = "true"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Exorcist : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        if (args.getOrNull(2)?.toBooleanStrictOrNull() ?: true)
            addFlag(target, "local")

        addTaskRandomTimer(target, {
            for (entity in LocationUtil.getEntitiesWithinRadius(target.location, 64.0)) {
                if (entity == target || entity !is LivingEntity || entity is ArmorStand)
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
                    PacketEvents.getAPI().playerManager.sendPacket(target, animation)
                    PacketEvents.getAPI().playerManager.sendPacket(target, look)
                    PacketEvents.getAPI().playerManager.sendPacket(target, head)
                } else for (player in target.world.players) {
                    // This entity should be within 64 blocks from *this* player
                    if (entity.location.distanceSquared(player.location) > 64.0 * 64.0)
                        continue

                    PacketEvents.getAPI().playerManager.sendPacket(player, animation)
                    PacketEvents.getAPI().playerManager.sendPacket(player, look)
                    PacketEvents.getAPI().playerManager.sendPacket(player, head)
                }
            }
        }, 0, 1, 3, true)

        return "&fEntities around &c${target.name} &fare now spazzing out"
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        return "&fEntities around &c${target.name} &fare no longer spazzing out"
    }
}