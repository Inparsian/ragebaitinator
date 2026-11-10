package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boombreak",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.IRON_PICKAXE,
    description = "Makes the next (or every) block a player breaks explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomBreak : BaseBoomTroll(), BlockBreakSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player breaks a block?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%breaks will now explode", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next block %c1%%player% %c2%breaks will no longer explode", target)
            .toString()

    override fun onBlockBreak(event: BlockBreakEvent) = kaboom(event, event.player, event.block.location, false)
}