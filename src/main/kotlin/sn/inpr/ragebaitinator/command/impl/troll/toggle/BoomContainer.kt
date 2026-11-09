package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.command.CommandSender
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boomcontainer",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.CHEST,
    description = "Makes the next (or every) container a player opens explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomContainer : BaseBoomTroll(), PlayerInteractSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player opens a container?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% container %c1%%player% %c2%opens will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next container %c1%%player% %c2%opens will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock ?: return
            if (block.state is Container)
                kaboom(event, event.player, block.location, false)
        }
    }
}