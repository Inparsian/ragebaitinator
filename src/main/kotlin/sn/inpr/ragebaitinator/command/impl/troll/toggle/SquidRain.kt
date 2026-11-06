package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Squid
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataType
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "squidrain",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.INK_SAC,
    description = "Rains squids upon a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class SquidRain : BaseToggleTroll(), EntityDamageSubscriber {
    val markKey = Main.makeKey("ragebaitinatorSquidRain")

    // Keep track of what squids are our doing instead of just making them invulnerable
    // The latter could potentially produce permanent immortal squids, we do not want that
    fun mark(entity: Entity) = entity.persistentDataContainer.set( markKey, PersistentDataType.BYTE, 1)
    fun isMarked(entity: Entity) = entity.persistentDataContainer.has(markKey, PersistentDataType.BYTE)

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        addTaskTimer(target, {
            val distance = MathUtil.random(3.0, 5.0)
            val yaw = MathUtil.random(0.0, 360.0)
            val [x, y, z] = Triple(MathUtil.rcos(yaw) * distance, 14.0, MathUtil.rsin(yaw) * distance)
            val squid = target.world.spawn(target.location.add(x, y, z), Squid::class.java)
            mark(squid)
            runTaskLater(squid::remove, 100)
        }, 0, 2, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now in an area with a 100% chance of squid rain")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer in squid rain")
            .put("player", target.name)
            .toString()

    // Prevent our squids from receiving damage, UNLESS they were alive for more than
    // 100 ticks, in which case this is an anomaly caused by a server restart
    override fun onEntityDamage(event: EntityDamageEvent) {
        event.cancelIf(isMarked(event.entity) && event.entity.ticksLived < 100)
    }
}