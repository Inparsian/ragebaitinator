package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Villager
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.MerchantInventory
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.InventoryCloseSubscriber
import sn.inpr.ragebaitinator.listeners.InventoryOpenSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.asRb

@CommandInfo(
    name = "scalper",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.EMERALD,
    description = "Villagers know a sucker when they see one.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Scalper : BaseToggleTroll(), InventoryOpenSubscriber, InventoryCloseSubscriber {
    private val markup = param<Int>("Markup", "How many items every price goes up by. A price can't be more than a full stack (min 1, max 64).", default = 64, range = 1..64)

    override val syntax = withTarget(markup)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "markup", args[markup])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being ripped off by every villager", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%gets the regular prices again", target)
            .toString()

    override fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player.asRb ?: return
        if (event.isCancelled || !hasPlayer(player))
            return

        val markup = getField<Int>(player, "markup") ?: 64
        (event.inventory as? MerchantInventory)?.merchant?.recipes?.forEach { it.specialPrice = markup }
    }

    override fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player.asRb ?: return
        if (!hasPlayer(player))
            return

        val merchant = (event.inventory as? MerchantInventory)?.merchant ?: return
        if (merchant !is Villager)
            merchant.recipes.forEach { it.specialPrice = 0 }
    }
}
