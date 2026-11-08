package sn.inpr.ragebaitinator.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.gui.impl.TrollMenu
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send

interface TrollToggleSubscriber
interface CommandHistorySubscriber

data class TrollGuiDataInput(
    val menu: TrollMenu,
    val syntax: CommandSyntax,
    val id: Int,
)

object GuiHandler : Listener {
    val menus: Map<Player, Menu> field = mutableMapOf()
    val inputs: Map<Player, TrollGuiDataInput> field = mutableMapOf()

    fun setMenu(player: Player, menu: Menu) = menus.put(player, menu)
    fun removeMenu(player: Player): Menu? = menus.remove(player)

    fun initDataInput(player: Player, input: TrollGuiDataInput) {
        inputs[player] = input
        val placeholders = Placeholders("gui.messages.fieldPrompt", "%prefix% %c2%Please enter a value for %c1%%field%")
            .put("field", input.syntax.purpose)
        send(player, placeholders)
        if (input.syntax.completions.isNotEmpty()) {
            val placeholders = Placeholders("gui.messages.fieldCompletions", "%prefix% %c2%Possible values: %c1%%completions%")
                .put("completions", input.syntax.completions.joinToString(", "))
            send(player, placeholders)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val topHolder = event.view.topInventory.holder
        if (topHolder is Menu) {
            // inventories outside ours are not our problem
            if (event.clickedInventory?.holder == topHolder) {
                event.isCancelled = true

                if (event.currentItem != null && !event.currentItem!!.type.isAir)
                    topHolder.onClick(event)
            }

            // shift clicks would move the item into this menu and get it destroyed
            else if(event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.isCancelled = true
            }
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

        val input = inputs.remove(event.player)
        if (input != null) {
            event.isCancelled = true

            var param: String
            when (input.syntax.type) {
                CommandSyntax.Type.STRING -> if (input.syntax.strictCompletion
                    && input.syntax.completions.none { it.equals(event.message, true) }
                    && input.syntax.completionAliases.none { it.equals(event.message, true) })
                {
                    val placeholders = Placeholders("gui.messages.fieldInvalidCompletion", "%prefix% &cThis value must be one of the possible values listed above")
                    send(event.player, placeholders)
                    input.menu.open()
                    return
                } else param = event.message

                // we can catch everything else with CommandSyntax's isValid
                else -> if (input.syntax.type.isValid(event.message, event.player))
                    param = event.message
                else {
                    val placeholders = if (input.syntax.type == CommandSyntax.Type.PLAYER)
                        Placeholders("gui.messages.fieldInvalidPlayer", "%prefix% &cPlayer(s) not found")
                    else
                        Placeholders("gui.messages.fieldInvalid", "%prefix% &cInvalid %type%")
                            .put("type", input.syntax.type.toString().lowercase())
                    send(event.player, placeholders)
                    input.menu.open()
                    return
                }
            }

            val placeholders = Placeholders("gui.messages.fieldSet", "%prefix% %c2%Set %c1%%field% %c2%to %c1%%value%")
                .put("field", input.syntax.purpose)
                .put("value", param)

            input.menu.params[input.id] = param
            send(event.player, placeholders)
            input.menu.open()
        }
    }
}