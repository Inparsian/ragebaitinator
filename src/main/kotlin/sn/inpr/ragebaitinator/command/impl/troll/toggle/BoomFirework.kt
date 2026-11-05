package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil.isFireworkBoost

@CommandInfo(
    name = "boomfirework",
    description = "Makes a player explode next (or every) time they try to firework boost while gliding.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player tries to firework boost while gliding?",
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
class BoomFirework : BaseToggleTroll(), PlayerInteractSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")
        if (args.getOrNull(3).toBoolean())
            addFlag(target, "destructive")

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now explode %next% time they firework boost")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer explode next time they firework boost")
            .put("player", target.name)
            .toString()

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if (players.containsKey(event.player.uniqueId) && isFireworkBoost(event)) {
            if (hasFlag(event.player, "destructive"))
                event.player.world.createExplosion(event.player.location, 5f, true)
            else {
                event.isCancelled = true
                LocationUtil.createSafeExplosion(event.player.location, 5f)
            }

            if (event.player.gameMode == GameMode.CREATIVE || event.player.gameMode == GameMode.SPECTATOR)
                event.player.health = 0.0

            if (!hasFlag(event.player, "persistent"))
                removePlayer(event.player)
        }
    }
}