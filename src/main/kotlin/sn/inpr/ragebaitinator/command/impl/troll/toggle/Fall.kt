package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerDeathSubscriber
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.UUID

@CommandInfo(
    name = "fall",
    description = "Makes a player fall indefinitely.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Fall : BaseToggleTroll(), EntityDamageSubscriber {
    val locations: MutableMap<UUID, Location> = mutableMapOf()

    fun getHighestLocation(location: Location): Location? {
        val world = location.world ?: return null
        var highestY = Int.MIN_VALUE
        for (x in -1..1) for (z in -1..1) {
            highestY = maxOf(highestY, world.getHighestBlockYAt(location.blockX + x, location.blockZ + z))
        }
        return location.clone().apply { y = highestY.toDouble() }
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val highest = getHighestLocation(target.location) ?: return "&cCould not find a location to fall from"
        locations[target.uniqueId] = highest.add(0.0, 20.0, 0.0)
        addTaskTimer(target, {
            val newLocation = locations[target.uniqueId] ?: return@addTaskTimer
            val currentLocation = target.location
            if (currentLocation.world != newLocation.world || currentLocation.y < newLocation.y - 15 || target.isOnGround) {
                val highest = getHighestLocation(currentLocation) ?: return@addTaskTimer
                val destination = highest.add(0.0, 20.0, 0.0).setDirection(currentLocation.direction)
                locations[target.uniqueId] = destination
                PlayerUtil.teleportRetainVelocity(target, destination)
            }
        }, 0, 1, true)
        return "&c${target.name} &fis now falling forever"
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        locations.remove(player.uniqueId)
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fhas been freed from the falling curse"

    // prevent premature fall damage despite our best efforts to prevent it proactively
    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (event.cause == EntityDamageEvent.DamageCause.FALL && players.containsKey(player.uniqueId))
            event.isCancelled = true
    }
}