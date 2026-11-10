package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "shuffle",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.BUNDLE,
    description = "Shuffles the player's inventory and hotbar",
)
class Shuffle : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val contents = target.storageContents.filterNotNullTo(mutableListOf<ItemStack?>())
        // make sure the size is exactly 36
        while (contents.size < 36) contents.add(ItemStack(Material.AIR))
        target.storageContents = contents.shuffled().toTypedArray()
        return placeholdersOr("init", "%prefix% %c2%Shuffled %c1%%player%%c2%'s inventory", target)
            .toString()
    }
}