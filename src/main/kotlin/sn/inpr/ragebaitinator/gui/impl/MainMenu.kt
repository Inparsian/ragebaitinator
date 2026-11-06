package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.gui.Menu

class MainMenu(player: Player) : Menu(player, false) {
    companion object {
        const val RELOAD_PERMISSION = "ragebaitinator.commands.reload"
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, "Ragebaitinator")

        addItem(inventory, 10, Material.BOOK, "§bPlayers", listOf("§7Manage players."), false)

        val reloadSlot = if (CommandHistory.hasHistoryAccess(player)) 14 else 13
        val historySlot = if (player.hasPermission(RELOAD_PERMISSION)) 12 else 13
        if(CommandHistory.hasHistoryAccess(player))
            addItem(inventory, historySlot, Material.PAPER, "§dHistory", listOf(
                "§7View command history. To date, a total of",
                "§c" + CommandHistory.size + "§7 command(s) have been executed."
            ), false)

        if (player.hasPermission(RELOAD_PERMISSION))
            addItem(inventory, reloadSlot, Material.LEVER, "§6Reload Configs", listOf("§7Reloads the plugin's configuration files."), false)

        addItem(inventory, 16, Material.REDSTONE, "§c§lRagebaitinator", listOf(
            "§7You are running §cRagebaitinator v"+ Main.instance().description.version,
            "§7made by §cInparsian§7!",
            "",
            "§7This plugin has a total of §c" + CommandManager.trolls.size + "§7 trolls!",
        ), false)

        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        when (event.currentItem?.itemMeta?.displayName) {
            "§bPlayers" -> PlayersMenu(player).open()
            "§6Reload Configs" -> if (player.hasPermission(RELOAD_PERMISSION))
                player.performCommand("ragebaitinator reload")
            "§dHistory" -> if(CommandHistory.hasHistoryAccess(player))
                HistoryMenu(player).open()
        }
    }
}