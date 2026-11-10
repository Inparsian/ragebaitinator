package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Furnace
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.listeners.FurnaceStartSmeltSubscriber
import sn.inpr.ragebaitinator.listeners.InventoryClickSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "slowcooker",
    category = CommandInfo.Category.WORLD,
    icon = Material.FURNACE,
    description = "A watched pot never boils. Makes the furnaces a player uses take forever to cook.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class SlowCooker : BaseToggleTroll(), InventoryClickSubscriber, FurnaceStartSmeltSubscriber {
    private companion object {
        // a day, as far as the furnace is concerned
        const val SLOW_COOK_TIME = 20 * 60 * 60 * 24
    }

    // whoever touched a furnace last gets blamed for it
    private val owners = ConcurrentHashMap<Block, UUID>()
    // how long each furnace that's been slowed down was supposed to take, for when it's let go
    private val originals = ConcurrentHashMap<Block, Int>()

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String
        = placeholdersOr("init", "%prefix% %c2%The furnaces %c1%%player% %c2%uses will now take forever to cook", target)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c2%The furnaces %c1%%player% %c2%uses cook normally again", target)
            .toString()

    // whatever they were cooking carries on from where it got to
    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        for ([block, owner] in owners.entries.toList()) {
            if (owner != player.uuid)
                continue

            owners.remove(block)
            release(block)
        }
    }

    private fun release(block: Block) {
        val original = originals.remove(block) ?: return
        val furnace = block.state as? Furnace ?: return
        furnace.cookTimeTotal = original
        furnace.update()
    }

    override fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val furnace = event.view.topInventory.holder as? Furnace ?: return
        val block = furnace.block

        if (hasPlayer(player))
            owners[block] = player.uniqueId
        // someone else has taken it over
        else if (owners.remove(block) != null)
            release(block)
    }

    // every item a furnace starts on is a new chance to slow it down
    override fun onFurnaceStartSmelt(event: FurnaceStartSmeltEvent) {
        if (!owners.containsKey(event.block))
            return

        originals[event.block] = event.totalCookTime
        event.totalCookTime = SLOW_COOK_TIME
    }
}
