package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.common.BaseBoomTroll
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber

@CommandInfo(
    name = "boomhit",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.DIAMOND_SWORD,
    description = "Makes the next (or every) entity a player hits explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player hits something?",
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
class BoomHit : BaseBoomTroll(), EntityDamageByEntitySubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        addBoomFlags(target, args)

        return placeholdersOr("init", "%prefix% %c2%%^next% entity %c1%%player% %c2%hits will now explode")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The next entity %c1%%player% %c2%hits will no longer explode")
            .put("player", target.name)
            .toString()

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val player = damager as? Player ?: (damager as? Projectile)?.shooter as? Player ?: return
        if (event.isCancelled)
            return

        kaboom(event, player, event.entity.location, false)
    }
}
