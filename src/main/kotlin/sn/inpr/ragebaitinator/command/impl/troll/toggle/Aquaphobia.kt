package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "aquaphobia",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.PUFFERFISH,
    description = "Makes a player allergic to water.",
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Aquaphobia : BaseToggleTroll(), PlayerMoveSubscriber {
    fun check(player: RbPlayer, block: Block) {
        if (hasPlayer(player)) {
            if (block.type.toString().contains("WATER")) {
                if (!hasTask(player)) {
                    setTaskTimer(player, {
                        val source = DamageSource.builder(DamageType.LAVA).build()
                        player.damage(3.0, source)
                    }, 0, 10)
                }
            } else {
                removeTask(player)
            }
        }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%now has aquagenic urticaria", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been cured of aquagenic urticaria", target)
            .toString()

    override fun onPlayerMove(event: PlayerMoveEvent) {
        check(event.player.rb, event.to?.block ?: return)
    }
}