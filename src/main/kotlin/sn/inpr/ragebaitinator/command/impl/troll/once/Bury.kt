package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerQuitEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.database.Blocks
import sn.inpr.ragebaitinator.listeners.PlayerDeathSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerQuitSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "bury",
    category = CommandInfo.Category.WORLD,
    icon = Material.DIRT,
    description = "Buries a player alive.",
)
class Bury : BaseTroll(), PlayerQuitSubscriber, PlayerDeathSubscriber {
    private fun is3x3(location: Location): Boolean {
        val block = location.block.location
        var xInRow = 0
        var zInRow = 0
        for (x in -1..1) if (block.clone().add(x.toDouble(), 0.0, 0.0).block.type.isSolid()) xInRow++
        for (z in -1..1) if (block.clone().add(0.0, 0.0, z.toDouble()).block.type.isSolid()) zInRow++
        return xInRow >= 3 && zInRow >= 3
    }

    private fun alreadyBuried(target: RbPlayer): Boolean {
        if (!Bukkit.isPrimaryThread() && !target.location.chunk.isLoaded)
            // possible async chunk loading! we'll return true.
            return true
        val location = target.location.clone()
        var solidsInRow = 0
        for (y in -1..3)
            if (is3x3(location.clone().add(0.0, y.toDouble(), 0.0))) solidsInRow++
        return solidsInRow == 4
    }

    override fun getConditions(target: RbPlayer)
            = Conditions().notDead(target)
                .notInVoid(target)
                .put("This player must not already be buried.", "alreadyBuried", alreadyBuried(target))

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val location = target.location.clone()
        var buryLocation: Location? = null
        var solidsInRow = 0
        for (y in LocationUtil.upHeightLimit downTo LocationUtil.downHeightLimit) {
            val block: Block = location.clone().add(0.0, y.toDouble(), 0.0).block
            if (!is3x3(block.location)) {
                solidsInRow = 0
            } else if (++solidsInRow == 4) {
                buryLocation = block.location.clone()
                break
            }
        }

        if (buryLocation == null) {
            val min = LocationUtil.downHeightLimit + 4
            buryLocation = if (target.location.y >= min)
                location.clone().add(0.0, -4.0, 0.0)
            else
                location.clone().toVector().setY(min).toLocation(location.world!!)
        }

        val fill = mutableListOf<Block>()
        for (y in 0..3) {
            for (x in -1..1) fill.add(buryLocation.clone().add(x.toDouble(), y.toDouble(), 0.0).block)
            for (z in -1..1) fill.add(buryLocation.clone().add(0.0, y.toDouble(), z.toDouble()).block)
        }
        Blocks.place(rollbackOwner(target), fill.filter { !it.type.isSolid }.map { it to Material.STONE })

        target.teleport(buryLocation.clone().add(0.0, 1.0, 0.0))
        return placeholdersOr("init", "%prefix% %c2%Buried %c1%%player% %c2%ALIVE MUAHAHAHAHAAHYAHAHAHA", target)
            .toString()
    }

    override fun onPlayerQuit(event: PlayerQuitEvent) = Blocks.release(rollbackOwner(event.player))
    override fun onPlayerDeath(event: PlayerDeathEvent) = Blocks.release(rollbackOwner(event.entity))
}