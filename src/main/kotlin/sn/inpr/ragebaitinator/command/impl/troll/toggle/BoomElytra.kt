package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityToggleGlideEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityToggleGlideSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "boomelytra",
    description = "Makes a player explode next (or every) time they try to use their elytra.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player tries to use their elytra?",
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
class BoomElytra : BaseToggleTroll(), EntityToggleGlideSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")
        if (args.getOrNull(3).toBoolean())
            addFlag(target, "destructive")

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now explode %next% time they start gliding")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer explode next time they start gliding")
            .put("player", target.name)
            .toString()

    override fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            if (hasFlag(player, "destructive"))
                player.world.createExplosion(player.location, 5f, true)
            else {
                event.isCancelled = true
                LocationUtil.createSafeExplosion(player.location, 5f)
            }

            if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR)
                player.health = 0.0

            if (!hasFlag(player, "persistent"))
                removePlayer(player)
        }
    }
}