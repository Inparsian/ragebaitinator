package sn.inpr.ragebaitinator.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.gui.def.Menu
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

interface TrollToggleSubscriber
interface CommandHistorySubscriber

/** Something that a menu needs typed in */
data class ChatInput(
    val menu: Menu,
    val field: String,
    val values: List<String> = emptyList(),
    val rejection: (text: String, player: Player) -> Placeholders? = { _, _ -> null },
    val accept: (text: String) -> Unit,
)

object GuiHandler : Listener {
    val menus: Map<Player, Menu> field = mutableMapOf()
    val inputs: Map<Player, ChatInput> field = mutableMapOf()

    fun setMenu(player: Player, menu: Menu) = menus.put(player, menu)
    fun removeMenu(player: Player): Menu? = menus.remove(player)

    fun ask(player: Player, input: ChatInput) {
        inputs[player] = input
        send(player, Placeholders("gui.messages.fieldPrompt", "%prefix% %c2%Please enter a value for %c1%%field%").put("field", input.field))
        if (input.values.isNotEmpty())
            send(player, Placeholders("gui.messages.fieldCompletions", "%prefix% %c2%Possible values: %c1%%completions%")
                .put("completions", input.values.joinToString(", ")))
        send(player, Placeholders("gui.messages.cancelHint", "%prefix% %c2%Type %c1%cancel %c2%to go back."))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        inputs.remove(event.player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        if (menus[player] == event.view.topInventory.holder)
            removeMenu(player)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val topHolder = event.view.topInventory.holder
        if (topHolder is Menu) {
            // inventories outside ours are not our problem
            if (event.clickedInventory?.holder == topHolder) {
                event.isCancelled = true

                if (event.currentItem != null && !event.currentItem!!.type.isAir)
                    topHolder.handleClick(event)
            }

            // shift clicks would move the item into this menu and get it destroyed
            else if(event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
                event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val top = event.view.topInventory
        if (top.holder is Menu && event.rawSlots.any { it < top.size })
            event.isCancelled = true
    }

    @EventHandler
    fun onAsyncChat(event: AsyncPlayerChatEvent) {
        if (!event.isAsynchronous) return

        val input = inputs.remove(event.player) ?: return
        event.isCancelled = true

        if (event.message.equals("cancel", true)) {
            send(event.player, Placeholders("gui.messages.cancelled", "%prefix% %c2%Cancelled."))
            input.menu.open()
            return
        }

        val rejection = input.rejection(event.message, event.player)
        if (rejection != null) {
            send(event.player, rejection)
            input.menu.open()
            return
        }

        // menus are only ever changed from the main thread
        runTask {
            input.accept(event.message)
            input.menu.open()
        }
    }
}
