package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "foolsgold",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.DIAMOND_ORE,
    description = "All that glitters is not diamond.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class FoolsGold : BaseToggleTroll(), BlockBreakSubscriber {
    private companion object {
        const val CHANCE = 0.15
        const val MIN_DISTANCE = 4
        const val MIN_SPACING = 12.0
        const val MAX_DISTANCE = 8
        const val MAX_VEIN_SIZE = 5
        const val MAX_TRACKED = 256

        val ores = mapOf(
            Material.STONE to Material.DIAMOND_ORE,
            Material.DEEPSLATE to Material.DEEPSLATE_DIAMOND_ORE
        )
        val faces = listOf(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "fakes", linkedSetOf<Block>())
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now find fake diamonds while mining", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer find fake diamonds", target)
            .toString()

    private fun restore(player: RbPlayer, block: Block) {
        if (player.isOnline && player.world == block.world)
            player.sendBlockChange(block.location, block.blockData)
    }

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        takeField<LinkedHashSet<Block>>(player, "fakes")?.forEach { restore(player, it) }
    }

    private fun vein(center: Block): List<Block> {
        val vein = mutableListOf(center)
        val size = MathUtil.random(2, MAX_VEIN_SIZE + 1)
        repeat(size * 4) {
            if (vein.size >= size)
                return@repeat

            val next = vein.random().getRelative(faces.random())
            if (next !in vein && next.type in ores)
                vein.add(next)
        }
        return vein
    }

    override fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val tracked = getField<LinkedHashSet<Block>>(player, "fakes")
        if (event.isCancelled || tracked == null || !MathUtil.randomBool(CHANCE))
            return

        // dig along wherever the player is facing, starting a few blocks further than the one they just mined
        val direction = player.eyeLocation.direction
        val world = event.block.world
        val origin = event.block.location.add(0.5, 0.5, 0.5)
        repeat(3) {
            val distance = MathUtil.random(MIN_DISTANCE.toDouble(), MAX_DISTANCE.toDouble())
            val spot = origin.clone().add(direction.clone().multiply(distance))
            // we won't load a chunk just for this
            if (!world.isChunkLoaded(spot.blockX shr 4, spot.blockZ shr 4))
                return@repeat

            val center = spot.block
            if (center.type !in ores)
                return@repeat
            if (tracked.any { it.world == world && it.location.distanceSquared(center.location) < MIN_SPACING * MIN_SPACING })
                return@repeat

            for (block in vein(center)) {
                player.sendBlockChange(block.location, (ores[block.type] ?: continue).createBlockData())
                tracked.add(block)
            }

            while (tracked.size > MAX_TRACKED) {
                val oldest = tracked.first()
                tracked.remove(oldest)
                restore(player.rb, oldest)
            }
            return
        }
    }
}
