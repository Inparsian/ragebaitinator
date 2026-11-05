@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.gui.impl

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.Menu
import sn.inpr.ragebaitinator.gui.PaginationMenu
import sn.inpr.ragebaitinator.gui.TrollGuiDataInput
import sn.inpr.ragebaitinator.utility.ItemUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.Reflection
import sn.inpr.ragebaitinator.utility.TypeUtil

private const val PAGE_SIZE = 36

class TrollsMenu(player: Player, val target: Player) : PaginationMenu(
    player,
    true,
    1,
    MathUtil.pages(CommandManager.trolls.size, PAGE_SIZE),
    PlayerMenu(player, target)
) {
    fun addItemCommons(troll: BaseTroll, item: ItemStack, lore: MutableList<String>, params: List<String>) {
        val word = if (troll is BaseToggleTroll) "toggle" else "execute"
        lore.addAll(listOf("", "§7Conditions:"))

        val conditions = troll.getConditions(target)
        if (conditions != null) {
            for ([humanReadable, _, predicate] in conditions.list) {
                lore.add("§8- §${if (predicate) "c" else "a"}$humanReadable")
                if (predicate) {
                    item.type = Material.BARRIER
                }
            }
        } else {
            lore.add("§fNone")
        }

        lore.addAll(listOf("", "§7Additional Parameters:"))
        if (params.isNotEmpty()) lore.addAll(params) else lore.add("§fNone")
        lore.addAll(listOf("", "§8Left click to $word."))

        if (troll.info.syntax.all { it.optional })
            lore.add("§8Right click to toggle with default parameters.")
    }

    fun trollItem(troll: BaseTroll): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta!!
        val lore: MutableList<String> = TypeUtil.splitAtLength(troll.info.description, 50, "§f")
        val params: MutableList<String> = mutableListOf()

        for (syntax in troll.info.syntax.filter { it.purpose.isNotEmpty() }) {
            val type = syntax.type.toString()
            val defaultPart = if (syntax.default.isNotEmpty()) {
                val color = if (syntax.type == CommandSyntax.Type.BOOLEAN) (if (syntax.default.toBoolean()) "§2" else "§4") else "§7"
                " §8(${color}${syntax.default}§8)"
            } else ""
            val requiredPart = if (!syntax.optional) " §c(Required)" else ""

            params.add("§8- §b${syntax.purpose} §7: §f" + type + defaultPart + requiredPart)
        }

        if (troll is BaseToggleTroll) {
            val enabled = troll.players.containsKey(player.uniqueId)
            if (enabled) ItemUtil.makeGlow(meta)

            meta.setDisplayName((if (enabled) "§a" else "§c") + troll.info.name)
            addItemCommons(troll, item, lore, params)

            // due to a weird bug with setting the type of book and quills i'm setting the type
            // after checking the conditions
            if (item.type != Material.BARRIER)
                item.type = (if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
                    Reflection.getMaterial("BOOK_AND_QUILL")
                else Material.WRITTEN_BOOK)!!
        } else {
            meta.setDisplayName("§6${troll.info.name}")
            addItemCommons(troll, item, lore, params)
        }

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    override fun getInventory(): Inventory {
        val items: MutableList<ItemStack> = mutableListOf()
        val trolls = CommandManager.trolls.chunked(PAGE_SIZE)[page - 1]

        for (command in trolls) {
            items.add(trollItem(command))
        }

        val inventory = Bukkit.createInventory(this, 45, "Trolls ($page/$pages)")
        inventory.contents = items.toTypedArray()
        addPaginationItems(inventory, page, pages)
        return inventory
    }

    override fun onClickItem(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val command = CommandManager.getCommand(event.currentItem?.itemMeta?.displayName?.substring(2))
        if (command != null) {
            if (command.info.syntax.any { it.purpose.isNotEmpty() }) {
                if (event.click == ClickType.LEFT) {
                    TrollMenu(player, command as BaseTroll, target).open()
                } else if (event.click == ClickType.RIGHT && command.info.syntax.all { it.optional }) {
                    player.performCommand("ragebaitinator ${command.info.name} ${target.name}")
                    open()
                }
            } else {
                player.performCommand("ragebaitinator ${command.info.name} ${target.name}")
                open()
            }
        }
    }

    override fun setbackCondition() = !target.isOnline()
    override fun setbackMenu() = PlayersMenu(player)
}

class TrollMenu(player: Player, val troll: BaseTroll, val target: Player) : Menu(player, true) {
    val syntax = troll.info.syntax.filter { it.purpose.isNotEmpty() }
    val params: MutableMap<Int, String> = mutableMapOf()

    init {
        for ([i, syntax] in syntax.withIndex()) {
            params[i] = syntax.default
        }
    }

    fun complete(): Boolean {
        for ([i, syntax] in syntax.withIndex()) {
            if (syntax.optional) continue
            if (!params.containsKey(i) || params[i].equals("")) return false
        }
        return true
    }

    fun item(): ItemStack {
        val item = ItemStack(if (troll is BaseToggleTroll) {
            if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
                Reflection.getMaterial("BOOK_AND_QUILL") ?: Material.WRITABLE_BOOK
            else Material.WRITABLE_BOOK
        } else Material.BOOK)
        val meta = item.itemMeta!!
        val lore = mutableListOf<String>()

        if (troll is BaseToggleTroll) {
            val enabled = troll.players.containsKey(player.uniqueId)
            if (enabled) ItemUtil.makeGlow(meta)
            meta.setDisplayName((if (enabled) "§a" else "§c") + troll.info.name)
        } else {
            meta.setDisplayName("§6${troll.info.name}")
        }

        lore.addAll(listOf("", "§7Conditions:"))
        val conditions = troll.getConditions(target)
        if (conditions != null) {
            for ([humanReadable, _, predicate] in conditions.list) {
                lore.add("§8- §${if (predicate) "c" else "a"}$humanReadable")
                if (predicate) {
                    item.type = Material.BARRIER
                }
            }
        } else {
            lore.add("§fNone")
        }

        meta.lore = lore
        item.itemMeta = meta
        return item
    }

    fun paperSlot(want: Int): Int {
        var slot = want + 12
        if (slot > 16) slot += 4
        if (slot > 25) slot += 4
        return slot.coerceIn(12, 34)
    }

    fun paramIndexFromSlot(slot: Int): Int? {
        val index = when (slot) {
            in 12..16 -> slot - 12
            in 21..25 -> slot - 16
            in 30..34 -> slot - 20
            else -> return null
        }
        return index.takeIf { it in syntax.indices }
    }

    override fun getInventory(): Inventory {
        val inventory = Bukkit.createInventory(this, 45, troll.info.name)
        addItem(inventory, 10, item())

        for ([i, syntax] in syntax.withIndex()) {
            val param = params.getOrDefault(i, "")
            val item = ItemStack(Material.PAPER)
            val meta = item.itemMeta!!
            val lore = if (syntax.description.isNotEmpty())
                TypeUtil.splitAtLength(troll.info.description, 50, "§f")
            else
                mutableListOf("§fNo description.")

            lore.addAll(listOf("", "§7Type: §f${syntax.type}", "§7Optional: ${if (syntax.optional) "§aYes" else "§cNo"}"))
            if (syntax.type == CommandSyntax.Type.BOOLEAN) {
                val isTrue = param.toBoolean()
                if (isTrue) ItemUtil.makeGlow(meta)
                meta.setDisplayName("§${if (isTrue) "a" else "c"}${syntax.purpose}")
            } else {
                meta.setDisplayName("§b${syntax.purpose}")
                lore.addAll(listOf("§7Current Value", if (param.isNotEmpty()) "§f$param" else "§4None"))
            }

            meta.lore = lore
            item.itemMeta = meta
            addItem(inventory, paperSlot(i), item)
        }

        if(complete())
            addItem(inventory, 19, Material.EMERALD_BLOCK, "§aConfirm", listOf(), false)
        else
            addItem(inventory, 19, Material.BARRIER, "§4Confirm", listOf(), false)
        addItem(inventory, 28, Material.REDSTONE_BLOCK, "§cBack", listOf(), false)

        return inventory
    }

    override fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        when (event.currentItem?.itemMeta?.displayName) {
            "§cBack" -> TrollsMenu(player, target).open()
            "§aConfirm" -> {
                val args: MutableList<String> = mutableListOf()
                for (param in params.values)
                    args.add(param)
                val finalCommand = "ragebaitinator ${troll.info.name} ${target.name} ${args.joinToString(" ")}"
                player.performCommand(finalCommand)
                open()
            }
            else -> {
                val slotId = paramIndexFromSlot(event.slot) ?: return
                val field = syntax[slotId]
                if (field.type == CommandSyntax.Type.BOOLEAN) {
                    params[slotId] = (!params.getOrDefault(slotId, "false").toBoolean()).toString()
                    open()
                } else {
                    close()
                    GuiHandler.initDataInput(player, TrollGuiDataInput(this, field, slotId))
                }
            }
        }
    }

    override fun setbackCondition() = !target.isOnline()
    override fun setbackMenu() = PlayersMenu(player)
}