package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockBreakAnimation
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.UUID

@CommandInfo(
    name = "crumble",
    description = "Naturally \"breaks\" blocks around a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
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
class Crumble : BaseToggleTroll() {
    data class CrumbleState(val entityId: Int, val stage: Byte, val vector: Vector)

    val blocks: MutableMap<UUID, MutableSet<CrumbleState>> = mutableMapOf()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        if (args.getOrNull(2)?.toBooleanStrictOrNull() ?: true)
            addFlag(target, "local")

        blocks.putIfAbsent(target.uniqueId, mutableSetOf())

        addTaskTimer(target, {
            val blockified = LocationUtil.blockify(target.location, false).toVector()
            val sphere = LocationUtil.sphere(blockified, 4.5)
            for (vector in sphere) {
                val block = blocks[target.uniqueId]?.firstOrNull { it.vector == vector }
                var [entityId, stage] = if (block != null) {
                    val delta = if (MathUtil.randomBool(0.2)) 1 else 0
                    val newStage = (block.stage + delta).toByte()
                    if (newStage < 9)
                        blocks[target.uniqueId]?.removeIf { it.vector == vector }
                    else
                        continue
                    Pair(block.entityId, newStage)
                } else {
                    Pair(MathUtil.random(0, Integer.MAX_VALUE), 1.toByte())
                }

                val vector3i = Vector3i(vector.x.toInt(), vector.y.toInt(), vector.z.toInt())
                val packet = WrapperPlayServerBlockBreakAnimation(entityId, vector3i, stage)
                if (hasFlag(target.uniqueId, "local"))
                    PacketEvents.getAPI().playerManager.sendPacket(target, packet)
                else
                    Bukkit.getOnlinePlayers().forEach { PacketEvents.getAPI().playerManager.sendPacket(it, packet) }

                val state = CrumbleState(entityId, stage, vector)
                blocks[target.uniqueId]?.add(state)
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c2%The world is now \"crumbling\" around %c1%%player%")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        blocks.remove(target.uniqueId)?.forEach { state ->
            val vector3i = Vector3i(state.vector.x.toInt(), state.vector.y.toInt(), state.vector.z.toInt())
            val packet = WrapperPlayServerBlockBreakAnimation(state.entityId, vector3i, -1)
            if (hasFlag(target.uniqueId, "local"))
                PacketEvents.getAPI().playerManager.sendPacket(target, packet)
            else
                Bukkit.getOnlinePlayers().forEach { PacketEvents.getAPI().playerManager.sendPacket(it, packet) }
        }

        return placeholdersOr("deinit", "%prefix% %c2%The world is no longer crumbling around %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}