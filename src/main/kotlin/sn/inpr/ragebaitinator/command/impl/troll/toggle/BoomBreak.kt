package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber

@CommandInfo(
    name = "boombreak",
    description = "Makes the next (or every) block a player breaks explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player breaks a block?",
            default = "false"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Destructive",
            description = "Should explosions be destructive?",
            default = "false"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomBreak : BaseBoomTroll(), BlockBreakSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        addBoomFlags(target, args)

        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%breaks will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The next block %c1%%player% %c2%breaks will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onBlockBreak(event: BlockBreakEvent) = kaboom(event, event.player, event.block.location, false)
}