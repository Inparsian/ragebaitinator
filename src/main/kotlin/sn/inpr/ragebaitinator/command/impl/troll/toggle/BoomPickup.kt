package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.EntityPickupItemSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boompickup",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.HOPPER_MINECART,
    description = "Makes the next (or every) item a player picks up explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomPickup : BaseBoomTroll(), EntityPickupItemSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player picks up an item?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% item %c1%%player% %c2%picks up will now explode", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next item %c1%%player% %c2%picks up will no longer explode", target)
            .toString()

    override fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        kaboom(event, player, event.item.location)
    }
}