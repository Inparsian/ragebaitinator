package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerBedEnterEvent
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.PlayerBedEnterSubscriber

@CommandInfo(
    name = "boombed",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.RED_BED,
    description = "Makes the next (or every) bed a player sleeps in explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player sleeps in a bed?",
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
class BoomBed : BaseBoomTroll(), PlayerBedEnterSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        addBoomFlags(target, args)

        return placeholdersOr("init", "%prefix% %c2%%^next% bed %c1%%player% %c2%sleeps in will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
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