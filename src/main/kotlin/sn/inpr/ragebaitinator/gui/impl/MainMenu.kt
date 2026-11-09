package sn.inpr.ragebaitinator.gui.impl

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.gui.CommandHistorySubscriber
import sn.inpr.ragebaitinator.gui.GuiText
import sn.inpr.ragebaitinator.gui.TrollToggleSubscriber
import sn.inpr.ragebaitinator.gui.centeredSlots
import sn.inpr.ragebaitinator.gui.def.Menu

private const val PLAYERS = "players"
private const val OVERVIEW = "overview"
private const val HISTORY = "history"
private const val RELOAD = "reload"

class MainMenu(player: Player) : Menu(player), TrollToggleSubscriber, CommandHistorySubscriber {
    companion object {
        const val RELOAD_PERMISSION = "ragebaitinator.commands.reload"
    }

    private class Button(val material: Material, val name: String, val lore: List<String>, val glowing: Boolean, val action: String)

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 27, GuiText.text("gui.main.title"))

        // the ones that can be used sit side by side in the middle, whichever of them that is
        val buttons = mutableListOf<Button>()
        buttons.add(Button(Material.BOOK, GuiText.text("gui.main.players.name"), GuiText.lines("gui.main.players.lore"), false, PLAYERS))

        val trolled = Bukkit.getOnlinePlayers().filter { activeTrolls(it).isNotEmpty() }
        buttons.add(Button(
            Material.REDSTONE_TORCH, GuiText.text("gui.main.overview.name"),
            GuiText.lines("gui.main.overview.lore", "players" to trolled.size.toString()), trolled.isNotEmpty(), OVERVIEW
        ))

        if (CommandHistory.hasHistoryAccess(player))
            buttons.add(Button(
                Material.PAPER, GuiText.text("gui.main.history.name"),
                GuiText.lines("gui.main.history.lore", "count" to CommandHistory.size.toString()), false, HISTORY
            ))

        if (player.hasPermission(RELOAD_PERMISSION))
            buttons.add(Button(Material.LEVER, GuiText.text("gui.main.reload.name"), GuiText.lines("gui.main.reload.lore"), false, RELOAD))

        buttons.zip(centeredSlots(buttons.size, 9)).forEach { [button, slot] ->
            addItem(inventory, slot, button.material, button.name, button.lore, button.glowing, button.action)
        }

        addItem(
            inventory, 22, Material.NETHER_STAR, GuiText.text("gui.main.info.name"),
            GuiText.lines("gui.main.info.lore", "trolls" to CommandManager.trolls.size.toString()), false
        )

        frame(inventory)
        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        when (actionOf(event.currentItem)) {
            PLAYERS -> PlayersMenu(player).open()
            OVERVIEW -> OverviewMenu(player).open()
            RELOAD -> if (player.hasPermission(RELOAD_PERMISSION))
                player.performCommand("ragebaitinator reload")
            HISTORY -> if (CommandHistory.hasHistoryAccess(player))
                HistoryMenu(player).open()
        }
    }
}
