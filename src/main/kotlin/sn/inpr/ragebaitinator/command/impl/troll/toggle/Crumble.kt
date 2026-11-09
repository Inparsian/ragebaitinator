package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
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

    data class CrumbleState(val entityId: Int, val stage: Byte, val vector: Vector)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[local]) addFlag(target, "local")

        setField(target, "blocks", mutableSetOf<CrumbleState>())

        addTaskTimer(target, {
            val blockified = LocationUtil.blockify(target.location, false).toVector()
            val sphere = LocationUtil.sphere(blockified, 4.5)
            for (vector in sphere) {
                val block = getField<MutableSet<CrumbleState>>(target, "blocks")?.firstOrNull { it.vector == vector }
                var [entityId, stage] = if (block != null) {
                    val delta = if (MathUtil.randomBool(0.2)) 1 else 0
                    val newStage = (block.stage + delta).toByte()
                    if (newStage < 9)
                        getField<MutableSet<CrumbleState>>(target, "blocks")?.removeIf { it.vector == vector }
                    else
                        continue
                    Pair(block.entityId, newStage)
                } else {
                    Pair(MathUtil.random(0, Integer.MAX_VALUE), 1.toByte())
                }

                val vector3i = Vector3i(vector.x.toInt(), vector.y.toInt(), vector.z.toInt())
                val packet = WrapperPlayServerBlockBreakAnimation(entityId, vector3i, stage)
                if (hasFlag(target, "local"))
                    target.sendPacket(packet)
                else
                    RbPlayer.online.sendPacket(packet)

                val state = CrumbleState(entityId, stage, vector)
                getField<MutableSet<CrumbleState>>(target, "blocks")?.add(state)
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c2%The world is now \"crumbling\" around %c1%%player%", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        takeField<MutableSet<CrumbleState>>(target, "blocks")?.forEach { state ->
            val vector3i = Vector3i(state.vector.x.toInt(), state.vector.y.toInt(), state.vector.z.toInt())
            val packet = WrapperPlayServerBlockBreakAnimation(state.entityId, vector3i, -1)
            if (hasFlag(target, "local"))
                target.sendPacket(packet)
            else
                RbPlayer.online.sendPacket(packet)
        }

        return placeholdersOr("deinit", "%prefix% %c2%The world is no longer crumbling around %c1%%player%", target)
            .toString()
    }
}