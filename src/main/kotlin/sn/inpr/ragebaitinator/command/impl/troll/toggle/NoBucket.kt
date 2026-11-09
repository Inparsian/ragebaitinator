package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerBucketEmptySubscriber
import sn.inpr.ragebaitinator.listeners.PlayerBucketFillSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "nobucket",
    category = CommandInfo.Category.WORLD,
    icon = Material.BUCKET,
    description = "Prevents a player from emptying or filling buckets.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class NoBucket : BaseToggleTroll(), PlayerBucketEmptySubscriber, PlayerBucketFillSubscriber {
    private val empty = param<Boolean>("Empty", "Should the player be prevented from emptying buckets?", default = true)
    private val fill = param<Boolean>("Fill", "Should the player be prevented from filling buckets?", default = true)

    override val syntax = withTarget(empty, fill)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val empty = args[empty]
        val fill = args[fill]

        return if (!empty && !fill) {
            removePlayer(target)
            Placeholders(language.bothFalse).toString()
        } else {
            if (empty) addFlag(target, "empty")
            if (fill) addFlag(target, "fill")
            placeholdersOr("init", "%prefix% %c1%%player% %c2%is no longer able to use buckets")
                .put("player", target.name)
                .toString()
        }
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now able to use buckets")
            .put("player", target.name)
            .toString()

    override fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        event.cancelIf(hasFlag(event.player, "empty"))
    }

    override fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        event.cancelIf(hasFlag(event.player, "fill"))
    }
}