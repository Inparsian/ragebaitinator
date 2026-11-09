package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.inventory.InventoryType
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "popup",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.CRAFTING_TABLE,
    description = "Opens a random inventory menu for a player.",
)
class Popup : BaseTroll() {
    companion object {
        val inventoryTypes = listOf(
            InventoryType.CHEST,
            InventoryType.DROPPER,
            InventoryType.HOPPER,
            InventoryType.DISPENSER,
            InventoryType.FURNACE,
            InventoryType.ANVIL,
        )
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.openInventory(Bukkit.createInventory(null, inventoryTypes.random()))
        return placeholdersOr("init", "%prefix% %c2%Opened a random inventory menu for %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}