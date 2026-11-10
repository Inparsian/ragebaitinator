package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockPlaceEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseLavaTroll
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "lavaplace",
    category = CommandInfo.Category.WORLD,
    icon = Material.NETHERRACK,
    description = "Building is a hot job. Makes the next (or every) block a player places turn into lava.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class LavaPlace : BaseLavaTroll(), BlockPlaceSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player places a block?", default = false)

    override val syntax = withTarget(persistent)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        addPersistentFlag(target, args[persistent])
        val persistent = hasFlag(target, "persistent")
        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%places will now turn into lava", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The blocks %c1%%player% %c2%places will no longer turn into lava", target)
            .toString()

    override fun onBlockPlace(event: BlockPlaceEvent) {
        if (!event.isCancelled)
            lavafy(event.player, event.blockPlaced)
    }
}
