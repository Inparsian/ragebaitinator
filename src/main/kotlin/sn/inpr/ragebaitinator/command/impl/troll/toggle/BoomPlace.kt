package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.BlockPlaceSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "boomplace",
    description = "Makes the next (or every) block a player places explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player places a block?",
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
class BoomPlace : BaseToggleTroll(), BlockPlaceSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")
        if (args.getOrNull(3).toBoolean())
            addFlag(target, "destructive")

        return placeholdersOr("init", "%prefix% %c2%%^next% block %c1%%player% %c2%places will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The next block %c1%%player% %c2%places will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onBlockPlace(event: BlockPlaceEvent) {
        if (players.containsKey(event.player.uniqueId)) {
            if (hasFlag(event.player, "destructive"))
                event.block.world.createExplosion(event.block.location, 5f, true)
            else {
                event.isCancelled = true
                LocationUtil.createSafeExplosion(event.block.location, 5f)
            }

            if (event.player.gameMode == GameMode.CREATIVE || event.player.gameMode == GameMode.SPECTATOR)
                event.player.health = 0.0

            if (!hasFlag(event.player, "persistent"))
                removePlayer(event.player)
        }
    }
}