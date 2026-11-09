package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boomeat",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.COOKED_BEEF,
    description = "Makes a player explode next (or every) time they eat something.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player eats something?",
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
class BoomEat : BaseBoomTroll(), PlayerItemConsumeSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        addBoomFlags(target, args)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now explode %next% time they eat something")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer explode next time they eat something")
            .put("player", target.name)
            .toString()

    // only food counts, and not milk or potions. the meal is spared, since it'd be the one thing left in the crater
    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type.isEdible)
            kaboom(event, event.player, event.player.location)
    }
}
