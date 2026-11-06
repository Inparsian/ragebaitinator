package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber

@CommandInfo(
    name = "spoiled",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.SPIDER_EYE,
    description = "Smells fine, looks fine... Poisons a player the next (or every) time they eat.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player eats something?",
            default = "false"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Spoiled : BaseToggleTroll(), PlayerItemConsumeSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")

        return placeholdersOr("init", "%prefix% %c2%%^next% food %c1%%player% %c2%eats will now be spoiled")
            .put("player", target.name)
            .put("next", if (persistent) "every" else "the next")
            .put("^next", if (persistent) "Every" else "The next")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c2%The next food %c1%%player% %c2%eats will no longer be spoiled")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        // milk and potions aren't food
        if (event.isCancelled || !hasPlayer(player) || !event.item.type.isEdible)
            return

        player.addPotionEffect(PotionEffect(PotionEffectType.POISON, 200, 1))
        if (!hasFlag(player, "persistent"))
            removePlayer(player)
    }
}
