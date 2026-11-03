package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerTeleportSubscriber

@CommandInfo(
    name = "aquaphobia",
    description = "Makes a player allergic to water.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Aquaphobia : BaseToggleTroll(), PlayerMoveSubscriber, PlayerTeleportSubscriber {
    fun check(player: Player, block: Block) {
        if (this.players.containsKey(player.uniqueId)) {
            if (block.type.toString().contains("WATER")) {
                if (!tasks.containsKey(player.uniqueId)) {
                    addTaskTimer(player, {
                        val source = DamageSource.builder(DamageType.LAVA).build()
                        player.damage(3.0, source)
                    }, 0, 10, false)
                }
            } else {
                removeTask(player.uniqueId)
            }
        }
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = "&c${target.name} &fnow has aquagenic urticaria"

    override fun deinit(sender: CommandSender, target: Player): String
        = "&c${target.name} &fhas been cured of aquagenic urticaria"

    override fun onRemovePlayer(player: Player, reason: String) {
        removeTask(player.uniqueId)
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        check(event.player, event.to?.block ?: return)
    }

    override fun onPlayerTeleport(event: PlayerTeleportEvent) {
        check(event.player, event.to?.block ?: return)
    }
}