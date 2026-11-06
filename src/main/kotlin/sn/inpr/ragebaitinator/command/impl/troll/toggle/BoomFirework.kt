package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.utility.PlayerUtil.isFireworkBoost

@CommandInfo(
    name = "boomfirework",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.FIREWORK_ROCKET,
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
class BoomFirework : BaseBoomTroll(), PlayerInteractSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        addBoomFlags(target, args)

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
        if (isFireworkBoost(event))
            kaboom(event, event.player, event.player.location)
    }
}