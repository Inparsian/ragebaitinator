package sn.inpr.ragebaitinator.command.impl.troll.once

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.PlayerDeathSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.utility.Reflection
import kotlin.math.absoluteValue

@CommandInfo(
    name = "hottub",
    description = "It's not lava it's just lukewarm water",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class HotTub : BaseTroll(), PlayerQuitSubscriber, PlayerDeathSubscriber {
    companion object {
        val LAVA = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
            Reflection.getMaterial("LAVA")
        else
            Material.LAVA

        val STATIONARY_LAVA = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
            Reflection.getMaterial("STATIONARY_LAVA")
        else
            Material.LAVA
    }

    val blockStates: Map<Player, MutableList<BlockState>> field = mutableMapOf()

    override fun getConditions(target: Player)
        = Conditions().notDead(target).notInVoid(target)

    fun getBowl(location: Location): List<Block> {
        val center = location.block.location
        val bowl: MutableList<Block> = mutableListOf()

        for(x in -2..2) for(z in -2..2) for(y in -1..0) {
            if (x.absoluteValue == 2 && z.absoluteValue == 2) continue
            if (y < 0 && (x.absoluteValue > 1 || z.absoluteValue > 1)) continue
            if (y == 0 && x.absoluteValue <= 1 && z.absoluteValue <= 1) continue
            bowl.add(center.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block)
        }
        return bowl
    }

    fun get3x3(location: Location): List<Block> {
        val center = location.block.location
        val by3: MutableList<Block> = mutableListOf()
        for(x in -1..1) for(z in -1..1) {
            by3.add(center.clone().add(x.toDouble(), 0.0, z.toDouble()).block)
        }
        return by3
    }

    fun fill(target: Player, block: Block, material: Material) {
        if (blockStates[target]!!.none { it.block.location == block.location })
            blockStates[target]!!.add(block.state)
        block.type = material
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val feet = target.location.add(0.0, -1.0, 0.0)
        val bowl = getBowl(feet)
        val lava = get3x3(feet)

        blockStates.putIfAbsent(target, mutableListOf())

        for (block in bowl.filter { !it.type.isSolid && it.type != LAVA && it.type != STATIONARY_LAVA })
            fill(target, block, Material.STONE)
        for (block in lava)
            fill(target, block, Material.LAVA)

        return "&c${target.name} &fhas been dropped into a nice and warm hot tub"
    }

    override fun onPlayerQuit(event: PlayerQuitEvent) {
        blockStates.remove(event.player)?.forEach { it.update(true, false) }
    }

    override fun onPlayerDeath(event: PlayerDeathEvent) {
        blockStates.remove(event.entity)?.forEach { it.update(true, false) }
    }

    override fun onDisable() {
        blockStates.values.forEach { snapshots -> snapshots.forEach { it.update(true, false) } }
        blockStates.clear()
    }
}