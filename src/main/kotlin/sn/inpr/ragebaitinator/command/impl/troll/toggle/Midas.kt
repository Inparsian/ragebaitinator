package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import kotlin.math.floor

@CommandInfo(
    name = "midas",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.GOLD_BLOCK,
    description = "Everything they touch turns to gold. Well, the floor does.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Midas : BaseToggleTroll(), PlayerMoveSubscriber {
    private val local = param<Boolean>("Local", "Whether this should only be visible to the player.", default = true)

    override val syntax = withTarget(local)

    private companion object {
        // the gold only exists on clients, so past this the oldest blocks revert to keep the trail from growing forever
        const val MAX_TRAIL = 1024
    }

    private val gold = Material.GOLD_BLOCK.createBlockData()

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[local]) addFlag(target, "local")

        setField(target, "trail", linkedSetOf<Block>())
        return placeholdersOr("init", "%prefix% %c2%The ground beneath %c1%%player% %c2%now turns to gold")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The ground beneath %c1%%player% %c2%is no longer turning to gold")
            .put("player", target.name)
            .toString()

    // whoever might have been shown the gold gets the real block back. it's harmless to resend it to everyone else
    private fun restore(block: Block) {
        val data = block.blockData
        block.world.players.forEach { it.sendBlockChange(block.location, data) }
    }

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        takeField<LinkedHashSet<Block>>(player, "trail")?.forEach(::restore)
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!hasPlayer(player))
            return

        val to = event.to ?: return
        val world = to.world ?: return
        val trail = getField<LinkedHashSet<Block>>(player, "trail") ?: return
        val block = world.getBlockAt(to.blockX, floor(to.y - 0.1).toInt(), to.blockZ)

        // move events are far more frequent than block changes, and a tile entity check isn't free
        val lastChecked = getField<Block>(player, "lastChecked")
        setField(player, "lastChecked", block)
        if (lastChecked == block)
            return
        if (block in trail || !block.type.isSolid || block.state is TileState)
            return

        trail.add(block)
        val viewers = if (hasFlag(player, "local")) listOf(player) else world.players
        viewers.forEach { it.sendBlockChange(block.location, gold) }

        if (trail.size > MAX_TRAIL) {
            val oldest = trail.first()
            trail.remove(oldest)
            restore(oldest)
        }
    }
}
