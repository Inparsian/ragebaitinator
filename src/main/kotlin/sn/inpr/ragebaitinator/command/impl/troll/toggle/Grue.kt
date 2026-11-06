package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber

@CommandInfo(
    name = "grue",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.COAL,
    description = "What are you talking about? The dark can't hurt you!",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Grue : BaseToggleTroll(), PlayerMoveSubscriber {
    fun mark(player: Player) {
        if (!hasFlag(player.uniqueId, "inDark")) {
            addFlag(player.uniqueId, "inDark")
            addTask(player.uniqueId, {
                if (player.location.block.lightLevel <= 2)
                    player.health = 0.0
            }, 60, true)
        }
    }

    fun unmark(player: Player) {
        removeFlag(player.uniqueId, "inDark")
        removeTask(player.uniqueId)
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        if (target.location.block.lightLevel <= 2)
            mark(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being stalked by a grue")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String =
        placeholdersOr("deinit", "%prefix% %c2%The grue has left %c1%%player%")
            .put("player", target.name)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (!hasPlayer(event.player)) return
        if (event.player.location.block.lightLevel <= 2)
            mark(event.player)
        else
            unmark(event.player)
    }
}