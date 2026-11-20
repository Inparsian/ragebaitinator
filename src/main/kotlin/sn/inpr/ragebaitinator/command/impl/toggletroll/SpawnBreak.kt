package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseSpawnMobTroll
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "spawnbreak",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.ZOMBIE_SPAWN_EGG,
    description = "Makes the next (or every) block a player breaks spawn a mob.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class SpawnBreak : BaseSpawnMobTroll(), BlockBreakSubscriber {
    private val type = mobParam("Type", "The mob to spawn.")
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player breaks a block?", default = false)

    override val syntax = withTarget(type, persistent)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val type = sanitize(args[type])
        val persistent = args[persistent]
        return addFields(target, type, persistent)
            ?: placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%breaks will now spawn a %c1%%type%", target)
                .put("next", if (persistent) "every" else "the next")
                .put("^next", if (persistent) "Every" else "The next")
                .put("type", type)
                .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next block %c1%%player% %c2%breaks will no longer spawn anything", target)
            .toString()

    override fun onBlockBreak(event: BlockBreakEvent) = peekaboo(event.player, event.block.location)
}