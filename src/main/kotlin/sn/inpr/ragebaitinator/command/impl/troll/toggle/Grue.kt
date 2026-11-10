package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "grue",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.COAL,
    description = "What are you talking about? The dark can't hurt you!",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Grue : BaseToggleTroll(), PlayerMoveSubscriber {
    fun mark(player: RbPlayer) {
        if (!hasFlag(player, "inDark")) {
            addFlag(player, "inDark")
            setTask(player, {
                if (player.location.block.lightLevel <= 2)
                    player.health = 0.0
            }, 60)
        }
    }

    fun unmark(player: RbPlayer) {
        removeFlag(player, "inDark")
        removeTask(player)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (target.location.block.lightLevel <= 2)
            mark(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being stalked by a grue", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c2%The grue has left %c1%%player%", target)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (!hasPlayer(event.player)) return
        if (event.player.location.block.lightLevel <= 2)
            mark(event.player.rb)
        else
            unmark(event.player.rb)
    }
}