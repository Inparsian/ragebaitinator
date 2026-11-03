package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryType

@CommandInfo(
    name = "popup",
    description = "Opens a random inventory menu for a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ]
)
class Popup : BaseTroll() {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val types = listOf(
            InventoryType.CHEST,
            InventoryType.DROPPER,
            InventoryType.HOPPER,
            InventoryType.DISPENSER,
            InventoryType.FURNACE,
            InventoryType.ANVIL,
        )

        target.openInventory(Bukkit.createInventory(null, types.random()))
        return "&fOpened a random inventory menu for &c${target.name}"
    }
}