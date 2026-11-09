package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerBucketEmptyEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerBucketEmptySubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "bucketswap",
    category = CommandInfo.Category.WORLD,
    icon = Material.LAVA_BUCKET,
    description = "Why is my water... lava?",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BucketSwap : BaseToggleTroll(), PlayerBucketEmptySubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String =
        placeholdersOr("init", "%prefix% %c1%%player%%c2%'s buckets are now having an identity crisis")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s buckets are now back to normal")
            .put("player", target.name)
            .toString()

    override fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        event.cancelIfAndThen(hasPlayer(event.player)) {
            val liquid = event.blockClicked.getRelative(event.blockFace)
            if (event.bucket == Material.WATER_BUCKET) liquid.type = Material.LAVA
            else if (event.bucket == Material.LAVA_BUCKET) liquid.type = Material.WATER
        }
    }
}