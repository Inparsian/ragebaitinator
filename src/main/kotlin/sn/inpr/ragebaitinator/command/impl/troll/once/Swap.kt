package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.Placeholders

@CommandInfo(
    name = "swap",
    description = "Swaps two players' locations (and inventories if you want to)",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Swap Inventories",
            description = "Whether or not to swap the players' inventories",
            default = "false"
        )
    ]
)
class Swap : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val target2: Player = args.getOrNull(2)?.let { targetPlayers(sender, it).firstOrNull() }
            ?: return Placeholders(language.playerNotFound).toString()
        val swapInventories: Boolean = args.getOrNull(3)?.toBoolean() ?: false
        val targetLocation = target.location
        target.teleport(target2.location)
        target2.teleport(targetLocation)
        if (swapInventories) {
            val targetInventory = target.inventory.contents
            target.inventory.contents = target2.inventory.contents
            target2.inventory.contents = targetInventory
        }
        return placeholdersOr("init", "%prefix% %c1%%playerA% %c2%and %c1%%playerB% %c2%have swapped places")
            .put("playerA", target.name)
            .put("playerB", target2.name)
            .toString()
    }
}