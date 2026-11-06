package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.listeners.InventoryClickSubscriber
import sn.inpr.ragebaitinator.listeners.InventoryDragSubscriber

@CommandInfo(
    name = "lockinv",
    description = "Look, but don't touch. Prevents a player from moving anything around in their inventory.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class LockInv : BaseToggleTroll(), InventoryClickSubscriber, InventoryDragSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player% %c2%can no longer touch their inventory")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%can touch their inventory again")
            .put("player", target.name)
            .toString()

    // our own menus have to keep working for anyone who's trolled and also happens to be using them
    private fun locked(player: Player, menu: Boolean) = hasPlayer(player) && !menu

    override fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        event.cancelIf(locked(player, event.view.topInventory.holder is Menu))
    }

    override fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        event.cancelIf(locked(player, event.view.topInventory.holder is Menu))
    }
}
