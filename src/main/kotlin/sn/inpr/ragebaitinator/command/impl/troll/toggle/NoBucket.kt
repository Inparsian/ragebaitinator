package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerBucketEmptySubscriber
import sn.inpr.ragebaitinator.listeners.PlayerBucketFillSubscriber
import sn.inpr.ragebaitinator.type.Placeholders

@CommandInfo(
    name = "nobucket",
    description = "Prevents a player from emptying or filling buckets.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Empty",
            description = "Should the player be prevented from emptying buckets?",
            default = "true"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Fill",
            description = "Should the player be prevented from filling buckets?",
            default = "true"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoBucket : BaseToggleTroll(), PlayerBucketEmptySubscriber, PlayerBucketFillSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val empty = args.getOrNull(2)?.toBooleanStrictOrNull() ?: true
        val fill = args.getOrNull(3)?.toBooleanStrictOrNull() ?: true

        return if (!empty && !fill) {
            removePlayer(target)
            Placeholders(language.bothFalse).toString()
        } else {
            if (empty) addFlag(target.uniqueId, "empty")
            if (fill) addFlag(target.uniqueId, "fill")
            placeholdersOr("init", "%prefix% %c1%%player% %c2%is no longer able to use buckets")
                .put("player", target.name)
                .toString()
        }
    }

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now able to use buckets")
            .put("player", target.name)
            .toString()

    override fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        event.cancelIf(hasFlag(event.player.uniqueId, "empty"))
    }

    override fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        event.cancelIf(hasFlag(event.player.uniqueId, "fill"))
    }
}