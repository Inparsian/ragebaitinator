package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "shuffle",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.BUNDLE,
    description = "Shuffles the player's inventory and hotbar",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false)
    ]
)
class Shuffle : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val contents = target.inventory.storageContents.filterNotNull().toMutableList()
        // make sure the size is exactly 36
        while (contents.size < 36) contents.add(ItemStack(Material.AIR))
        target.inventory.storageContents = contents.shuffled().toTypedArray()
        return placeholdersOr("init", "%prefix% %c2%Shuffled %c1%%player%%c2%'s inventory")
            .put("player", target.name)
            .toString()
    }
}