package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

@CommandInfo(
    name = "midas",
    description = "Everything they touch turns to gold. Well, the floor does.",
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
class Midas : BaseToggleTroll(), PlayerMoveSubscriber {
    private companion object {
        // the gold only exists on clients, so past this the oldest blocks revert to keep the trail from growing forever
        const val MAX_TRAIL = 1024
    }

    private val gold = Material.GOLD_BLOCK.createBlockData()
    private val trails = ConcurrentHashMap<UUID, LinkedHashSet<Block>>()
    private val lastChecked = ConcurrentHashMap<UUID, Block>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        if (args.getOrNull(2)?.toBooleanStrictOrNull() ?: true)
            addFlag(target, "local")

        trails[target.uniqueId] = linkedSetOf()
        return placeholdersOr("init", "%prefix% %c2%The ground beneath %c1%%player% %c2%now turns to gold")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The ground beneath %c1%%player% %c2%is no longer turning to gold")
            .put("player", target.name)
            .toString()

    // whoever might have been shown the gold gets the real block back. it's harmless to resend it to everyone else
    private fun restore(block: Block) {
        val data = block.blockData
        block.world.players.forEach { it.sendBlockChange(block.location, data) }
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        lastChecked.remove(player.uniqueId)
        trails.remove(player.uniqueId)?.forEach(::restore)
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (!hasPlayer(player))
            return

        val to = event.to ?: return
        val world = to.world ?: return
        val trail = trails[player.uniqueId] ?: return
        val block = world.getBlockAt(to.blockX, floor(to.y - 0.1).toInt(), to.blockZ)

        // move events are far more frequent than block changes, and a tile entity check isn't free
        if (lastChecked.put(player.uniqueId, block) == block)
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
