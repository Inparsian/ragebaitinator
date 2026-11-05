package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityPickupItemSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "boompickup",
    description = "Makes the next (or every) item a player picks up explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player picks up an item?",
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
class BoomPickup : BaseToggleTroll(), EntityPickupItemSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")
        if (args.getOrNull(3).toBoolean())
            addFlag(target, "destructive")

        return placeholdersOr("init", "%prefix% %c2%%^next% item %c1%%player% %c2%picks up will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The next item %c1%%player% %c2%picks up will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (players.containsKey(player.uniqueId)) {
            runTaskLater({
                if (hasFlag(player, "destructive"))
                    event.item.world.createExplosion(event.item.location, 5f, true)
                else {
                    event.isCancelled = true
                    LocationUtil.createSafeExplosion(event.item.location, 5f)
                }

                if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR)
                    player.health = 0.0

                if (!hasFlag(player, "persistent"))
                    removePlayer(player)
            }, 1)
        }
    }
}