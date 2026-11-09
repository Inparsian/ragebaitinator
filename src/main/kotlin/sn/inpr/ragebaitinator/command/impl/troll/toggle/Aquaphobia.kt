package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.player.PlayerMoveEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

@CommandInfo(
    name = "aquaphobia",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.PUFFERFISH,
    description = "Makes a player allergic to water.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Aquaphobia : BaseToggleTroll(), PlayerMoveSubscriber {
    fun check(player: RbPlayer, block: Block) {
        if (hasPlayer(player)) {
            if (block.type.toString().contains("WATER")) {
                if (!tasks.containsKey(player)) {
                    addTaskTimer(player, {
                        val source = DamageSource.builder(DamageType.LAVA).build()
                        player.damage(3.0, source)
                    }, 0, 10, false)
                }
            } else {
                removeTask(player)
            }
        }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%now has aquagenic urticaria")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been cured of aquagenic urticaria")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        removeTask(player)
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        check(event.player.rb, event.to?.block ?: return)
    }
}