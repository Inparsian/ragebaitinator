package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseLavaTroll
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "lavabreak",
    category = CommandInfo.Category.WORLD,
    icon = Material.MAGMA_BLOCK,
    description = "Mining is a hot job. Makes the next (or every) block a player breaks turn into lava.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class LavaBreak : BaseLavaTroll(), BlockBreakSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player breaks a block?", default = false)

    override val syntax = withTarget(persistent)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        addPersistentFlag(target, args[persistent])
        val persistent = hasFlag(target, "persistent")
        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%breaks will now turn into lava")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The blocks %c1%%player% %c2%breaks will no longer turn into lava")
            .put("player", target.name)
            .toString()

    override fun onBlockBreak(event: BlockBreakEvent) {
        if (!event.isCancelled)
            lavafy(event.player, event.block)
    }
}
