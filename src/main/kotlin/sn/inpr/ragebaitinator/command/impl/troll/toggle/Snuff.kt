package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockPlaceEvent
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "snuff",
    category = CommandInfo.Category.WORLD,
    icon = Material.TORCH,
    description = "Torches have to go out eventually!",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Snuff : BaseToggleTroll(), BlockPlaceSubscriber {
    private val seconds = param<Int>("Seconds", "The longest a light lasts. It goes out after somewhere between one second and this (min 2, max 300).", default = 8, range = 2..300)

    override val syntax = withTarget(seconds)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "seconds", args[seconds])
        return placeholdersOr("init", "%prefix% %c1%%player%'s %c2%lights will now go out", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%'s %c2%lights will stay on again", target)
            .toString()

    private fun isTorch(type: Material) = type == Material.TORCH || type.name.endsWith("_TORCH")
    private fun isCandle(type: Material) = type.name.endsWith("CANDLE")
    private fun isCampfire(type: Material) = type.name.endsWith("CAMPFIRE")

    override fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled || !hasPlayer(event.player))
            return

        val block = event.blockPlaced
        val type = block.type
        if (!isTorch(type) && !isCandle(type) && !isCampfire(type))
            return

        val seconds = getField<Int>(event.player, "seconds") ?: 8
        runTaskLater({ snuff(block, type) }, MathUtil.random(20L, seconds * 20L))
    }

    private fun snuff(block: Block, type: Material) {
        val world = block.world
        if (!world.isChunkLoaded(block.x shr 4, block.z shr 4) || block.type != type)
            return

        if (isTorch(type)) {
            block.breakNaturally()
        } else {
            val data = block.blockData as? Lightable ?: return
            if (!data.isLit)
                return

            data.isLit = false
            block.blockData = data
        }

        val center = block.location.add(0.5, 0.5, 0.5)
        world.spawnParticle(Particle.SMOKE, center, 8, 0.15, 0.15, 0.15, 0.02)
        world.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1f)
    }
}
