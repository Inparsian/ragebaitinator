package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerTeleportSubscriber
import sn.inpr.ragebaitinator.utility.PlayerUtil.inVoid
import java.util.*

@CommandInfo(
    name = "void",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.END_PORTAL_FRAME,
    description = "Banishes a player into the void.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ],
    incompatible = [ "cage" ]
)
class Void : BaseToggleTroll(), EntityDamageSubscriber, PlayerMoveSubscriber, PlayerTeleportSubscriber {
    private val locations = mutableMapOf<UUID, Location>()

    fun get65536(player: Player): Location {
        val location = player.location.clone()
        location.y = -65536.0
        return location
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        locations[target.uniqueId] = target.location
        target.teleport(get65536(target))
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been banished into the void")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been returned from the void")
            .put("player", target.name)
            .toString()
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        player.fallDistance = 0f // prevents an oopsie
        locations.remove(player.uniqueId)?.let { player.teleport(it) }
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        event.cancelIf(hasPlayer(player) && inVoid(player, -2.0))
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (hasPlayer(event.player) && !inVoid(event.player, -2.0))
            event.player.teleport(get65536(event.player))
    }

    override fun onPlayerTeleport(event: PlayerTeleportEvent) {
        event.cancelIf(hasPlayer(event.player) && inVoid(event.player, -2.0))
    }
}