package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax

@CommandInfo(
    name = "popup",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.CRAFTING_TABLE,
    description = "Opens a random inventory menu for a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ]
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

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        target.openInventory(Bukkit.createInventory(null, inventoryTypes.random()))
        return placeholdersOr("init", "%prefix% %c2%Opened a random inventory menu for %c1%%player%")
            .put("player", target.name)
            .toString()
    }
}