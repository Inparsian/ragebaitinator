package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "annoy",
    description = "Hrmm. Annoys a player with villager sounds.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Annoy : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        addTaskRandomTimer(target, {
            target.playSound(target.location, "entity.villager.ambient", 1f, 1f)
        }, 0, 100, 200, true)
        return "&c${target.name} &fis now hearing voices in their head"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&fThe voices of the villagers have left &c${target.name} &falone... for now"
}