package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boomeat",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.COOKED_BEEF,
    description = "Makes a player explode next (or every) time they eat something.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomEat : BaseBoomTroll(), PlayerItemConsumeSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player eats something?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now explode %next% time they eat something", target)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%will no longer explode next time they eat something", target)
            .toString()

    // only food counts, and not milk or potions. the meal is spared, since it'd be the one thing left in the crater
    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        if (event.item.type.isEdible)
            kaboom(event, event.player, event.player.location)
    }
}
