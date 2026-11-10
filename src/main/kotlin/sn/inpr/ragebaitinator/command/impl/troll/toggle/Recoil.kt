package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.entity.EntityShootBowEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.EntityShootBowSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb

@CommandInfo(
    name = "recoil",
    category = CommandInfo.Category.COMBAT,
    icon = Material.CROSSBOW,
    description = "Bows were not meant to be this powerful. Every bow or crossbow shot kicks the shooter backwards.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Recoil : BaseToggleTroll(), EntityShootBowSubscriber {
    private val strength = param<Double>("Strength", "How hard every shot kicks them back (min 0.1, max 5).", default = 1.0, range = 0.1..5.0)

    override val syntax = withTarget(strength)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "strength", args[strength])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now gets kicked back by every shot they take", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can shoot without being knocked over again", target)
            .toString()

    override fun onEntityShootBow(event: EntityShootBowEvent) {
        val player = event.entity.asRb ?: return
        if (event.isCancelled || !hasPlayer(player))
            return

        val body = player.bukkit ?: return
        val strength = getField<Double>(player, "strength") ?: 1.0
        val kick = body.location.direction.multiply(-strength * (0.5 + event.force))
        kick.y += 0.2 * strength.coerceAtMost(2.0)
        body.velocity = body.velocity.add(kick)
    }
}
