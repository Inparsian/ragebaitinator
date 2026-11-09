package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "fall",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.LEATHER_BOOTS,
    description = "Makes a player fall indefinitely.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Fall : BaseToggleTroll(), EntityDamageSubscriber {
    fun getHighestLocation(location: Location): Location? {
        val world = location.world ?: return null
        var highestY = Int.MIN_VALUE
        for (x in -1..1) for (z in -1..1) {
            highestY = maxOf(highestY, world.getHighestBlockYAt(location.blockX + x, location.blockZ + z))
        }
        return location.clone().apply { y = highestY.toDouble() }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val highest = getHighestLocation(target.location) ?: return "&cCould not find a location to fall from"
        setField(target, "location", highest.add(0.0, 20.0, 0.0))
        addTaskTimer(target, {
            val newLocation = getField<Location>(target, "location") ?: return@addTaskTimer
            val currentLocation = target.location
            if (currentLocation.world != newLocation.world || currentLocation.y < newLocation.y - 15 || target.isOnGround) {
                val highest = getHighestLocation(currentLocation) ?: return@addTaskTimer
                val destination = highest.add(0.0, 20.0, 0.0).setDirection(currentLocation.direction)
                setField(target, "location", destination)
                target.teleportRetainVelocity(destination)
            }
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now falling forever")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been freed from the falling curse")
            .put("player", target.name)
            .toString()

    // prevent premature fall damage despite our best efforts to prevent it proactively
    override fun onEntityDamage(event: EntityDamageEvent) {
        event.cancelIf(event.cause == EntityDamageEvent.DamageCause.FALL && hasPlayer(event.entity as? Player ?: return))
    }
}