package sn.inpr.ragebaitinator.command.impl.toggletroll

import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.sendPacket
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "crumble",
    category = CommandInfo.Category.WORLD,
    icon = Material.CRACKED_STONE_BRICKS,
    description = "Naturally \"breaks\" blocks around a player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Crumble : BaseToggleTroll() {
    private val local = param<Boolean>("Local", "Whether this should only be visible to the player.", default = true)

    override val syntax = withTarget(local)

    data class CrumbleState(val entityId: Int, var stage: Byte, val vector: Vector) {
        private fun sendPacket(player: RbPlayer, local: Boolean, destroyStage: Byte = stage) {
            val blockPosition = Vector3i(vector.x.toInt(), vector.y.toInt(), vector.z.toInt())
            val packet = WrapperPlayServerBlockBreakAnimation(entityId, blockPosition, destroyStage)
            if (local)
                player.sendPacket(packet)
            else
                RbPlayer.online.sendPacket(packet)
        }

        fun revert(player: RbPlayer, local: Boolean) = sendPacket(player, local, -1)
        fun send(player: RbPlayer, local: Boolean) = sendPacket(player, local)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[local]) addFlag(target, "local")

        setField(target, "blocks", mutableListOf<CrumbleState>())

        setTaskTimer(target, {
            val blockified = LocationUtil.blockify(target.location, false).toVector()
            val sphere = LocationUtil.sphere(blockified, 4.5)
            for (vector in sphere) {
                if (!vector.toLocation(target.world).block.type.isSolid)
                    continue
                val blocks = getField<MutableList<CrumbleState>>(target, "blocks") ?: return@setTaskTimer
                val block = blocks.firstOrNull { it.vector == vector }
                if (block != null) {
                    val delta = if (MathUtil.randomBool(0.1)) 1 else 0
                    val newStage = (block.stage + delta).toByte().coerceAtMost(9)
                    block.stage = newStage
                    block.send(target, args[local])
                } else {
                    val [entityId, stage] = Pair(MathUtil.random(0, Integer.MAX_VALUE), 1.toByte())
                    blocks.add(CrumbleState(entityId, stage, vector))
                    if (blocks.size >= 256)
                        blocks.removeFirst().revert(target, args[local])
                }
            }
        }, 0, 1)

        return placeholdersOr("init", "%prefix% %c2%The world is now \"crumbling\" around %c1%%player%", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The world is no longer crumbling around %c1%%player%", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        val list = takeField<MutableList<CrumbleState>>(player, "blocks")
        while (list?.isNotEmpty() ?: false)
            list.removeFirst().revert(player, hasFlag(player, "local"))
    }
}