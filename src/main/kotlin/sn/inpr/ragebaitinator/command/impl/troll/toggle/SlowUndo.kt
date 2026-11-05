package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.block.data.BlockData
import org.bukkit.command.CommandSender
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "slowundo",
    description = "Gradually undoes the block changes a player makes.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Interval",
            description = "The amount of ticks between each undone block change.",
            default = "40"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class SlowUndo : BaseToggleTroll(), BlockBreakSubscriber, BlockPlaceSubscriber {
    private companion object {
        // anything past this undoes the oldest change right away instead of letting the queue grow
        const val MAX_PENDING = 512
    }

    private data class Change(
        val block: Block,
        val previous: BlockData,
        val placed: Material?,
        val drops: Collection<ItemStack> = emptyList(),
        val exp: Int = 0
    )

    private val pending = ConcurrentHashMap<UUID, ArrayDeque<Change>>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val interval = (args.getOrNull(2)?.toIntOrNull() ?: 40).coerceIn(1, 1200).toLong()
        val queue = ArrayDeque<Change>()
        pending[target.uniqueId] = queue

        addTaskTimer(target, { queue.pollFirst()?.let(::undo) }, interval, interval)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s block changes will now be slowly undone")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s block changes are no longer being undone")
            .put("player", target.name)
            .toString()

    // whatever is still queued stays as the player left it. the blocks they broke were never given back, so they
    // get what those blocks would have dropped instead
    override fun onRemovePlayer(player: Player, reason: String) {
        val queue = pending.remove(player.uniqueId) ?: return
        for (change in queue)
            release(change)
    }

    private fun release(change: Change) {
        val block = change.block
        if (change.placed != null || !block.world.isChunkLoaded(block.x shr 4, block.z shr 4))
            return

        val location = block.location.add(0.5, 0.5, 0.5)
        change.drops.forEach { block.world.dropItemNaturally(location, it) }
        if (change.exp > 0)
            block.world.spawn(location, ExperienceOrb::class.java) { it.experience = change.exp }
    }

    private fun undo(change: Change) {
        val block = change.block
        // we won't load a chunk just for this
        if (!block.world.isChunkLoaded(block.x shr 4, block.z shr 4))
            return

        if (change.placed != null) {
            if (block.type == change.placed)
                block.setBlockData(change.previous, true)
        } else if (block.isEmpty || block.isLiquid) {
            // no physics, or a door's other half would pop off as an item
            block.setBlockData(change.previous, false)
        } else {
            // something else took the spot, so the block isn't coming back
            release(change)
        }
    }

    private fun track(player: Player, change: Change) {
        val queue = pending[player.uniqueId] ?: return
        queue.addLast(change)
        if (queue.size > MAX_PENDING)
            undo(queue.pollFirst())
    }

    override fun onBlockBreak(event: BlockBreakEvent) {
        // containers would lose their contents if they were put back, so they get left alone
        if (event.isCancelled || !hasPlayer(event.player) || event.block.state is TileState)
            return

        val player = event.player
        val survival = player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE
        val drops = if (survival) event.block.getDrops(player.inventory.itemInMainHand, player) else emptyList()
        track(player, Change(event.block, event.block.blockData, null, drops, event.expToDrop))
        // the block is probably coming back, so the drops are held until it's known whether it does
        event.isDropItems = false
        event.expToDrop = 0
    }

    override fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled || !hasPlayer(event.player) || event.blockPlaced.state is TileState)
            return

        track(event.player, Change(event.blockPlaced, event.blockReplacedState.blockData, event.blockPlaced.type))
    }
}
