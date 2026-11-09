package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "butterfingers",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.SLIME_BALL,
    description = "Greasy hands. The item a player is holding sometimes flies out of their hand when they swing, mine or attack.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Butterfingers : BaseToggleTroll(), PlayerInteractSubscriber, BlockBreakSubscriber, EntityDamageByEntitySubscriber {
    private val chance = param<Double>("Chance", "The chance that an action makes them drop their item (min 0, max 1).", default = 0.1, range = 0.0..1.0)

    override val syntax = withTarget(chance)

    private companion object {
        const val COOLDOWN_MILLIS = 250L
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "chance", args[chance])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has butterfingers", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has a firm grip again", target)
            .toString()

    private fun roll(player: Player) {
        val chance = getField<Double>(player, "chance") ?: return
        val now = System.currentTimeMillis()
        if (now - (getField<Long>(player, "lastRoll") ?: 0L) < COOLDOWN_MILLIS)
            return
        setField(player, "lastRoll", now)

        if (!MathUtil.randomBool(chance) || player.inventory.itemInMainHand.type.isAir)
            return

        // not in the middle of the event, which might still be using that item
        Main.runTask {
            val held = player.inventory.itemInMainHand
            if (!player.isOnline || held.type.isAir)
                return@runTask

            player.inventory.setItemInMainHand(null)
            val direction = player.location.direction
            val item = player.world.dropItem(player.eyeLocation.subtract(0.0, 0.3, 0.0), held)
            item.velocity = direction.multiply(MathUtil.random(0.15, 0.4))
                .add(org.bukkit.util.Vector(MathUtil.random(-0.1, 0.1), MathUtil.random(0.1, 0.3), MathUtil.random(-0.1, 0.1)))
            item.pickupDelay = 40
        }
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK)
            roll(event.player)
    }

    override fun onBlockBreak(event: BlockBreakEvent) = roll(event.player)

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        roll(player)
    }
}
