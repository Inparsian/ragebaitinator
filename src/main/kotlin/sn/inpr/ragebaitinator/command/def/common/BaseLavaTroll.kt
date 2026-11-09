package sn.inpr.ragebaitinator.command.def.common

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.database.Blocks
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

/**
 * A toggle troll that turns a block into lava whenever a certain event happens. The lava is rolled back after a
 * while, along with anything that flowed out of it, and there's a flag for persistence.
 */
abstract class BaseLavaTroll : BaseToggleTroll() {
    private companion object {
        const val LAVA_LIFETIME_TICKS = 200L
    }

    fun addPersistentFlag(target: RbPlayer, persistent: Boolean) {
        if (persistent)
            addFlag(target, "persistent")
    }

    /**
     * Turns [block] into lava once the event that's being handled is over, so that the block it replaces is the one
     * that's actually there by then.
     */
    fun lavafy(player: Player, block: Block): Unit = lavafy(player.rb, block)

    fun lavafy(player: RbPlayer, block: Block) {
        if (!hasPlayer(player))
            return

        if (!hasFlag(player, "persistent"))
            removePlayer(player)

        runTask {
            val owner = rollbackOwner(player)
            Blocks.place(owner, listOf(block to Material.LAVA))
            runTaskLater({ Blocks.release(owner, listOf(block)) }, LAVA_LIFETIME_TICKS)
        }
    }

    // anything still lava when they leave or the plugin stops goes back right away. the rest has its own timer, which
    // is why turning this off doesn't touch it
    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        if (reason == RemoveReason.LOGOUT || reason == RemoveReason.CLEARED)
            Blocks.release(rollbackOwner(player))
    }
}
