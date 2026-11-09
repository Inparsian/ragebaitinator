package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerBedEnterEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerBedEnterSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boombed",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.RED_BED,
    description = "Makes the next (or every) bed a player sleeps in explode.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomBed : BaseBoomTroll(), PlayerBedEnterSubscriber {
    private val persistent = param<Boolean>("Persistent", "Should this continue to be active after the player sleeps in a bed?", default = false)
    private val destructive = param<Boolean>("Destructive", "Should explosions be destructive?", default = false)

    override val syntax = withTarget(persistent, destructive)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val persistent = args[persistent]
        addBoomFlags(target, persistent, args[destructive])

        return placeholdersOr("init", "%prefix% %c2%%^next% bed %c1%%player% %c2%sleeps in will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The next bed %c1%%player% %c2%sleeps in will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        // This will already happen in the nether & the end
        if (event.player.world.environment != World.Environment.NORMAL)
            return

        kaboom(event, event.player, event.bed.location)
    }
}