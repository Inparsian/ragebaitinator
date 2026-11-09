package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "annoy",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.VILLAGER_SPAWN_EGG,
    description = "Hrmm. Annoys a player with villager sounds.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Annoy : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        addTaskRandomTimer(target, {
            target.playSound(target.location, "entity.villager.ambient", 1f, 1f)
        }, 0, 100, 200, true)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now hearing voices in their head")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The voices of the villagers have left %c1%%player% %c2%alone... for now")
            .put("player", target.name)
            .toString()
}