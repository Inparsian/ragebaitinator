package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.database.Blocks
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.*
import kotlin.math.absoluteValue

@CommandInfo(
    name = "sinkhole",
    category = CommandInfo.Category.WORLD,
    icon = Material.SUSPICIOUS_SAND,
    description = "Conjures a sinkhole beneath a player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Sinkhole : BaseToggleTroll(), EntityDamageSubscriber {
    // unique per sinkhole, so a delayed release can't touch the blocks of a later sinkhole on the same player
    data class SinkholeState(
        var startLocation: Location,
        var pastPlayerLocation: Location,
        val rollbackOwner: String = "sinkhole:${UUID.randomUUID()}"
    )

    fun floorBelowPlayer(player: RbPlayer): Boolean {
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

    override fun getConditions(target: RbPlayer) = Conditions()
        .notDead(target)
        .notInVoid(target)
        .put("There must be a floor below the player.", "noFloor", !floorBelowPlayer(target))

    fun sink(cylinder: Set<Vector>, player: RbPlayer) {
        player.isFlying = false
        val owner = getField<SinkholeState>(player, "sinkhole")?.rollbackOwner ?: return
        val blocks = cylinder.map { it.toLocation(player.world).block }.filter { it.type.isSolid }
        if (blocks.isEmpty())
            return

        Blocks.place(owner, blocks.map { it to Material.AIR })
        runTaskLater({ Blocks.release(owner, blocks) }, 60L)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "sinkhole", SinkholeState(target.location, target.location))

        val initCylinder = LocationUtil.cylinder(target.location.toVector().add(Vector(0.0, -2.0, 0.0)), 2.0, 3)
        sink(initCylinder, target)

        addTaskTimer(target, {
            if (hasField(target, "sinkhole")) {
                if (PlayerUtil.inVoid(target) || !floorBelowPlayer(target)) {
                    removePlayer(target, "done")
                    return@addTaskTimer
                }

                val sinkhole = getField<SinkholeState>(target, "sinkhole")!!
                val holeLocation = sinkhole.startLocation.toVector().setY(target.location.y).toLocation(target.world)

                if (target.location.y < sinkhole.pastPlayerLocation.y) {
                    sink(LocationUtil.cylinder(holeLocation.toVector().add(Vector(0.0, -3.0, 0.0)), 2.0, 4), target)
                    sinkhole.pastPlayerLocation = target.location
                } else if (target.location.y > sinkhole.pastPlayerLocation.y) {
                    target.teleportRetainVelocity(sinkhole.pastPlayerLocation.clone().add(0.0, -1.0, 0.0))
                }

                val diffX = (target.location.x - holeLocation.x).absoluteValue
                val diffZ = (target.location.z - holeLocation.z).absoluteValue
                if (diffX > 1.28 || diffZ > 1.28) {
                    val newLocation = holeLocation.clone()
                    newLocation.y = target.location.y
                    newLocation.yaw = target.location.yaw
                    newLocation.pitch = target.location.pitch
                    target.teleportRetainVelocity(newLocation)
                }
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been sent on a journey to the center of the earth", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been returned back to the surface", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        if (!arrayOf("done", "death").contains(reason)) {
            player.fallDistance = 0f
            player.teleport(getField<SinkholeState>(player, "sinkhole")?.startLocation ?: player.location)
        }

        takeField<SinkholeState>(player, "sinkhole")?.let { Blocks.release(it.rollbackOwner) }
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val uuid = (event.entity as? Player ?: return).uniqueId
        event.cancelIf(event.cause == EntityDamageEvent.DamageCause.FALL && hasField(uuid, "sinkhole"))
    }
}