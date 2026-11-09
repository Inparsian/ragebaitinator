package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockPlaceEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boomplace",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.COBBLESTONE,
    description = "Makes the next (or every) block a player places explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomPlace : BaseBoomTroll(), BlockPlaceSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player places a block?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%places will now explode", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next block %c1%%player% %c2%places will no longer explode", target)
            .toString()

    override fun onBlockPlace(event: BlockPlaceEvent) = kaboom(event, event.player, event.block.location)
}