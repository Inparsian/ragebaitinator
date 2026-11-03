package sn.inpr.ragebaitinator.gui

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.scheduler.BukkitTask
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.gui.impl.TrollMenu
import sn.inpr.ragebaitinator.utility.ChatUtil.send

data class TrollGuiDataInput(
    val menu: TrollMenu,
    val syntax: CommandSyntax,
    val id: Int,
)

object GuiHandler : Listener {
    val tasks: Map<Player, BukkitTask>
        field = mutableMapOf()
    val inputs: Map<Player, TrollGuiDataInput>
        field = mutableMapOf()

    fun addTask(player: Player, task: BukkitTask) = tasks.putIfAbsent(player, task)
    fun removeTask(player: Player): BukkitTask? = tasks.remove(player)

    fun initDataInput(player: Player, input: TrollGuiDataInput) {
        inputs[player] = input
        send(player, "Please enter a value for &c${input.syntax.purpose}&f.")
        if (input.syntax.completions.isNotEmpty())
            send(player, "§7Possible values: &c${input.syntax.completions.joinToString(", ")}")
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
                    send(event.player, "&cThis value must be one of the possible values listed above")
                    input.menu.open()
                    return
                } else param = event.message

                // we can catch everything else with CommandSyntax's isValid
                else -> if (input.syntax.type.isValid(event.message, event.player))
                    param = event.message
                else {
                    val message = if (input.syntax.type == CommandSyntax.Type.PLAYER)
                        "&cPlayer(s) not found"
                    else
                        "&cInvalid ${input.syntax.type.toString().lowercase()}"
                    send(event.player, message)
                    input.menu.open()
                    return
                }
            }

            input.menu.params[input.id] = param
            send(event.player, "&fSet &c${input.syntax.purpose} &fto &c$param")
            input.menu.open()
        }
    }
}