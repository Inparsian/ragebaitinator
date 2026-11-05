package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerRespawnSubscriber
import sn.inpr.ragebaitinator.type.Structure
import sn.inpr.ragebaitinator.utility.LocationUtil
import java.util.*

@CommandInfo(
    name = "cage",
    description = "Puts a player in a cage.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "At Sender",
            description = "Whether or not to put the cage at your feet/head.",
            default = "false"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "void" ]
)
class Cage : BaseToggleTroll(), PlayerMoveSubscriber, PlayerRespawnSubscriber, BlockBreakSubscriber {
    private val cage: Structure = Structure()
        .add(Material.COAL_BLOCK, 0, 0, 0)
        .add(Material.COAL_BLOCK, 0, 0, 1)
        .add(Material.COAL_BLOCK, 1, 0, 0)
        .add(Material.COAL_BLOCK, 1, 0, 1)
        .add(Material.COAL_BLOCK, 0, 0, -1)
        .add(Material.COAL_BLOCK, -1, 0, 0)
        .add(Material.COAL_BLOCK, -1, 0, -1)
        .add(Material.COAL_BLOCK, -1, 0, 1)
        .add(Material.COAL_BLOCK, 1, 0, -1)
        .add(Material.COAL_BLOCK, 0, 3, 0)
        .add(Material.COAL_BLOCK, 0, 3, 1)
        .add(Material.COAL_BLOCK, 1, 3, 0)
        .add(Material.COAL_BLOCK, 1, 3, 1)
        .add(Material.COAL_BLOCK, 0, 3, -1)
        .add(Material.COAL_BLOCK, -1, 3, 0)
        .add(Material.COAL_BLOCK, -1, 3, -1)
        .add(Material.COAL_BLOCK, -1, 3, 1)
        .add(Material.COAL_BLOCK, 1, 3, -1)
        .add(Material.GLASS_PANE, 0, 1, 1)
        .add(Material.GLASS_PANE, 0, 1, -1)
        .add(Material.GLASS_PANE, 1, 1, 0)
        .add(Material.GLASS_PANE, -1, 1, 0)
        .add(Material.GLASS_PANE, 1, 1, 1)
        .add(Material.GLASS_PANE, 1, 1, -1)
        .add(Material.GLASS_PANE, -1, 1, 1)
        .add(Material.GLASS_PANE, -1, 1, -1)
        .add(Material.GLASS_PANE, 0, 2, 1)
        .add(Material.GLASS_PANE, 0, 2, -1)
        .add(Material.GLASS_PANE, 1, 2, 0)
        .add(Material.GLASS_PANE, -1, 2, 0)
        .add(Material.GLASS_PANE, 1, 2, 1)
        .add(Material.GLASS_PANE, 1, 2, -1)
        .add(Material.GLASS_PANE, -1, 2, 1)
        .add(Material.GLASS_PANE, -1, 2, -1)
    private val maxY = LocationUtil.upHeightLimit - (cage.height + 1)
    private val cages = mutableMapOf<UUID, Location>()

    private fun rollbackOwner(uuid: UUID) = "cage:$uuid"

    fun getViableLocation(player: Player): Location? {
        val blockified = LocationUtil.blockify(player.location, true)
        if (blockified.y >= maxY) {
            // there's usually no intersection at world height, but we won't take chances
            val atWorldHeight = blockified.clone()
            atWorldHeight.y = maxY.toDouble()
            if (!cage.intersects(atWorldHeight))
                return atWorldHeight
        } else {
            if (blockified.y < LocationUtil.downHeightLimit)
                blockified.y = LocationUtil.downHeightLimit.toDouble()

            for (i in 0 until maxY) {
                val location = blockified.clone().add(0.0, i.toDouble(), 0.0)
                if (!cage.intersects(location))
                    return location
            }
        }

        return null
    }

    override fun getConditions(target: Player): Conditions {
        val intersection = !cages.containsKey(target.uniqueId) && getViableLocation(target) == null
        return Conditions().notInVoid(target)
            .put("The cage shouldn't be in the way of anything.", "intersection", intersection)
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val atSender = args.getOrNull(2)?.toBoolean() ?: false
        val cageLocation = if (atSender && sender is Player) {
            val blockified = LocationUtil.blockify(sender.location, true)
            val feet = blockified.clone().add(0.0, -cage.height.toDouble(), 0.0)
            val head = blockified.clone().add(0.0, 2.0, 0.0)
            feet.y = feet.y.coerceIn(LocationUtil.downHeightLimit.toDouble(), cage.height.toDouble())
            head.y = head.y.coerceIn(LocationUtil.downHeightLimit.toDouble(), cage.height.toDouble())
            if (!cage.intersects(feet))
                feet
            else if (cage.intersects(head))
                head
            else
                return "&cThe cage would hit something!"
        } else {
            getViableLocation(target) ?: return "&cThe cage would hit something!"
        }

        cage.build(rollbackOwner(target.uniqueId), cageLocation)
        cages[target.uniqueId] = cageLocation
        target.teleport(cageLocation.clone().add(0.0, 1.0, 0.0))

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now in a cage")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now free")
            .put("player", target.name)
            .toString()

    fun isOutsideCage(player: Player, to: Location): Boolean {
        val cageLocation = cages[player.uniqueId] ?: return false
        return to.distance(cageLocation.clone().add(0.0, 1.0, 0.0)) > 1.25
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        event.cancelIf(isOutsideCage(event.player, event.to ?: return))
    }

    override fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (cages.containsKey(event.player.uniqueId))
            runTaskLater({ event.player.teleport(cages[event.player.uniqueId] ?: return@runTaskLater) }, 1)
    }

    override fun onBlockBreak(event: BlockBreakEvent) {
        // prevent cages from being broken
        for (cageLocation in cages.values) {
            for (vector in cage.vectors) {
                val dist = cageLocation.block.location.add(vector).distance(event.block.location)
                if (dist < 1.0) {
                    event.isCancelled = true
                    break
                }
            }
        }
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        if (cages.remove(player.uniqueId) != null)
            cage.rollback(rollbackOwner(player.uniqueId))
    }
}