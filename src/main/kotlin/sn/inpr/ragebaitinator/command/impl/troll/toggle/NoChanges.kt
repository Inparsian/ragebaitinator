package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import sn.inpr.ragebaitinator.type.Placeholders

@CommandInfo(
    name = "nochanges",
    description = "Prevents a player from making block changes.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Break",
            description = "Should the player be prevented from breaking blocks?",
            default = "true"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Place",
            description = "Should the player be prevented from placing blocks?",
            default = "true"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoChanges : BaseToggleTroll(), BlockBreakSubscriber, BlockPlaceSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val mine = args.getOrNull(2)?.toBooleanStrictOrNull() ?: true // literally screw the 'break' keyword
        val place = args.getOrNull(3)?.toBooleanStrictOrNull() ?: true

        return if (!mine && !place) {
            removePlayer(target)
            Placeholders(language.bothFalse).toString()
        } else {
            if (mine) addFlag(target.uniqueId, "mine")
            if (place) addFlag(target.uniqueId, "place")
            placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer make block changes")
                .put("player", target.name)
                .toString()
        }
    }

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now able to make block changes")
            .put("player", target.name)
            .toString()

    override fun onBlockBreak(event: BlockBreakEvent) {
        event.cancelIf(hasFlag(event.player.uniqueId, "mine"))
    }
    
    override fun onBlockPlace(event: BlockPlaceEvent) {
        event.cancelIf(hasFlag(event.player.uniqueId, "place"))
    }
}