package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.InventoryClickSubscriber
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "pumpkinhead",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.CARVED_PUMPKIN,
    description = "Can't see a thing. Puts a (client-side) pumpkin on a player's head.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Pumpkinhead : BaseToggleTroll(), PacketEventSendSubscriber, InventoryClickSubscriber {
    private companion object {
        const val HELMET_MENU_SLOT = 5
        const val HELMET_INVENTORY_SLOT = 39
        const val VIEW_DISTANCE_SQUARED = 128.0 * 128.0
    }

    private val wearers = ConcurrentHashMap.newKeySet<Int>()
    private fun pumpkin(): ItemStack = ItemStack.builder().type(ItemTypes.CARVED_PUMPKIN).amount(1).build()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        wearers.add(target.entityId)
        show(target, pumpkin())
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has a pumpkin on their head")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has taken the pumpkin off")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        wearers.remove(player.entityId)
        if (player.isOnline && reason != "logout")
            show(player, SpigotConversionUtil.fromBukkitItemStack(player.inventory.helmet ?: org.bukkit.inventory.ItemStack(Material.AIR)))
    }

    private fun show(player: Player, helmet: ItemStack) {
        val api = PacketEvents.getAPI().playerManager
        api.sendPacket(player, WrapperPlayServerSetPlayerInventory(HELMET_INVENTORY_SLOT, helmet))

        val equipment = WrapperPlayServerEntityEquipment(player.entityId, listOf(Equipment(EquipmentSlot.HELMET, helmet)))
        val location = player.location
        for (viewer in player.world.players)
            if (viewer != player && viewer.canSee(player) && viewer.location.distanceSquared(location) <= VIEW_DISTANCE_SQUARED)
                api.sendPacket(viewer, equipment)
    }

    // the server doesn't know about the pumpkin, so every time it says what is on the player's head, it has to be
    // corrected, or the pumpkin would come off whenever the inventory is synced
    override fun onPacketSend(event: PacketSendEvent) {
        if (wearers.isEmpty())
            return

        when (event.packetType) {
            PacketType.Play.Server.SET_PLAYER_INVENTORY -> if (event.user.entityId in wearers) {
                val wrapper = WrapperPlayServerSetPlayerInventory(event)
                if (wrapper.slot == HELMET_INVENTORY_SLOT)
                    wrapper.stack = pumpkin()
            }
            PacketType.Play.Server.SET_SLOT -> if (event.user.entityId in wearers) {
                val wrapper = WrapperPlayServerSetSlot(event)
                if (wrapper.windowId == 0 && wrapper.slot == HELMET_MENU_SLOT)
                    wrapper.item = pumpkin()
            }
            PacketType.Play.Server.WINDOW_ITEMS -> if (event.user.entityId in wearers) {
                val wrapper = WrapperPlayServerWindowItems(event)
                if (wrapper.windowId == 0 && wrapper.items.size > HELMET_MENU_SLOT)
                    wrapper.items = wrapper.items.toMutableList().also { it[HELMET_MENU_SLOT] = pumpkin() }
            }
            PacketType.Play.Server.ENTITY_EQUIPMENT -> {
                val wrapper = WrapperPlayServerEntityEquipment(event)
                if (wrapper.entityId in wearers)
                    wrapper.equipment = wrapper.equipment.filter { it.slot != EquipmentSlot.HELMET } +
                            Equipment(EquipmentSlot.HELMET, pumpkin())
            }
        }
    }

    override fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        event.cancelIf(
            player.entityId in wearers && event.clickedInventory == player.inventory &&
                event.slotType == InventoryType.SlotType.ARMOR && event.slot == HELMET_INVENTORY_SLOT
        )
    }
}
