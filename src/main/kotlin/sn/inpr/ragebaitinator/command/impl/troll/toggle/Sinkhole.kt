package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.*
import kotlin.math.absoluteValue

@CommandInfo(
    name = "sinkhole",
    description = "Conjures a sinkhole beneath a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Sinkhole : BaseToggleTroll(), EntityDamageSubscriber {
    data class SinkholeState(var startLocation: Location, var pastPlayerLocation: Location)

    private val blocks: MutableMap<UUID, Queue<BlockState>> = mutableMapOf()
    private val sinkholes: MutableMap<UUID, SinkholeState> = mutableMapOf()

    fun floorBelowPlayer(player: Player): Boolean {
        if (!Bukkit.isPrimaryThread() && !player.location.chunk.isLoaded)
        // err we dunno really
            return false

        for (y in player.location.y.toInt() downTo LocationUtil.downHeightLimit) {
            val vector: Vector = player.location.toVector().setY(y)
            val cylinder: Set<Vector> = LocationUtil.cylinder(vector, 8.0, 1)
            for (vec in cylinder) {
                val loc: Location = vec.toLocation(player.world)
                if (loc.block.type.isSolid)
                    return true
            }
        }

        return false
    }

    override fun getConditions(target: Player) = Conditions()
        .notDead(target)
        .notInVoid(target)
        .put("There must be a floor below the player.", "noFloor", !floorBelowPlayer(target))

    fun sink(cylinder: Set<Vector>, player: Player) {
        player.isFlying = false
        for (vector in cylinder) {
            val location: Location = vector.toLocation(player.world)
            if (location.block.type.isSolid()) {
                blocks.putIfAbsent(player.uniqueId, LinkedList())
                blocks[player.uniqueId]?.add(location.block.state)
                location.block.type = Material.AIR

                runTaskLater({
                    if (blocks.containsKey(player.uniqueId))
                        blocks[player.uniqueId]?.poll()?.update(true, false)
                }, 60L)
            }
        }
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        sinkholes[target.uniqueId] = SinkholeState(target.location, target.location)

        val initCylinder = LocationUtil.cylinder(target.location.toVector().add(Vector(0.0, -2.0, 0.0)), 2.0, 3)
        sink(initCylinder, target)

        addTaskTimer(target, {
            if (sinkholes.containsKey(target.uniqueId)) {
                if (PlayerUtil.inVoid(target) || !floorBelowPlayer(target)) {
                    removePlayer(target, "done")
                    return@addTaskTimer
                }

                val sinkhole = sinkholes[target.uniqueId]!!
                val holeLocation = sinkhole.startLocation.toVector().setY(target.location.y).toLocation(target.world)

                if (target.location.y < sinkhole.pastPlayerLocation.y) {
                    sink(LocationUtil.cylinder(holeLocation.toVector().add(Vector(0.0, -3.0, 0.0)), 2.0, 4), target)
                    sinkhole.pastPlayerLocation = target.location
                } else if (target.location.y > sinkhole.pastPlayerLocation.y) {
                    PlayerUtil.teleportRetainVelocity(target, sinkhole.pastPlayerLocation.clone().add(0.0, -1.0, 0.0))
                }

                val diffX = (target.location.x - holeLocation.x).absoluteValue
                val diffZ = (target.location.z - holeLocation.z).absoluteValue
                if (diffX > 1.28 || diffZ > 1.28) {
                    val newLocation = holeLocation.clone()
                    newLocation.y = target.location.y
                    newLocation.yaw = target.location.yaw
                    newLocation.pitch = target.location.pitch
                    PlayerUtil.teleportRetainVelocity(target, newLocation)
                }
            }
        }, 0, 1, true)

        return "&c${target.name} &fhas been sent on a journey to the center of the earth"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fhas been returned back to the surface"

    override fun onRemovePlayer(player: Player, reason: String) {
        if (!arrayOf("done", "death").contains(reason)) {
            player.fallDistance = 0f
            player.teleport(sinkholes[player.uniqueId]?.startLocation ?: player.location)
        }

        sinkholes.remove(player.uniqueId)
        blocks.remove(player.uniqueId)?.forEach { it.update(true, false) }
    }

    override fun onDisable() {
        blocks.values.forEach { it.forEach { block -> block.update(true, false) } }
        blocks.clear()
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (event.cause == EntityDamageEvent.DamageCause.FALL && sinkholes.containsKey(player.uniqueId))
            event.isCancelled = true
    }
}