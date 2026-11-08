package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.Menu

private const val PLAYERS = "players"
private const val HISTORY = "history"
private const val RELOAD = "reload"

class MainMenu(player: Player) : Menu(player) {
    companion object {
        const val RELOAD_PERMISSION = "ragebaitinator.commands.reload"
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, GuiText.text("gui.main.title"))

        addItem(inventory, 10, Material.BOOK, GuiText.text("gui.main.players.name"), GuiText.lines("gui.main.players.lore"), false, PLAYERS)

        // whichever of these two the player can't use leaves the other in the middle
        val reloadSlot = if (CommandHistory.hasHistoryAccess(player)) 14 else 13
        val historySlot = if (player.hasPermission(RELOAD_PERMISSION)) 12 else 13
        if (CommandHistory.hasHistoryAccess(player))
            addItem(
                inventory, historySlot, Material.PAPER, GuiText.text("gui.main.history.name"),
                GuiText.lines("gui.main.history.lore", "count" to CommandHistory.size.toString()), false, HISTORY
            )

        if (player.hasPermission(RELOAD_PERMISSION))
            addItem(inventory, reloadSlot, Material.LEVER, GuiText.text("gui.main.reload.name"), GuiText.lines("gui.main.reload.lore"), false, RELOAD)

        addItem(
            inventory, 16, Material.REDSTONE, GuiText.text("gui.main.info.name"),
            GuiText.lines("gui.main.info.lore", "trolls" to CommandManager.trolls.size.toString()), false
        )

        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        when (actionOf(event.currentItem)) {
            PLAYERS -> PlayersMenu(player).open()
            RELOAD -> if (player.hasPermission(RELOAD_PERMISSION))
                player.performCommand("ragebaitinator reload")
            HISTORY -> if (CommandHistory.hasHistoryAccess(player))
                HistoryMenu(player).open()
        }
    }
}
