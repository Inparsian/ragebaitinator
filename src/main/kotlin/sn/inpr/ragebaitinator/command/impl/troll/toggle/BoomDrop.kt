package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerDropItemEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerDropItemSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boomdrop",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.TNT,
    description = "Makes the next (or every) item a player drops explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomDrop : BaseBoomTroll(), PlayerDropItemSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player drops an item?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% item %c1%%player% %c2%drops will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next item %c1%%player% %c2%drops will no longer explode")
            .put("player", target.name)
            .toString()

    // the item is let go of first, so it's the one that explodes and not something that never left their hand
    override fun onPlayerDropItem(event: PlayerDropItemEvent) = kaboom(event, event.player, event.itemDrop.location, false)
}
