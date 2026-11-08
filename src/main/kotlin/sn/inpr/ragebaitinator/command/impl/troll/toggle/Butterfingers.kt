package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "butterfingers",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.SLIME_BALL,
    description = "Greasy hands. The item a player is holding sometimes flies out of their hand when they swing, mine or attack.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Chance",
            description = "The chance that an action makes them drop their item (min 0, max 1).",
            default = "0.1"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Butterfingers : BaseToggleTroll(), PlayerInteractSubscriber, BlockBreakSubscriber, EntityDamageByEntitySubscriber {
    private companion object {
        const val COOLDOWN_MILLIS = 250L
    }

    private val chances = ConcurrentHashMap<UUID, Double>()
    private val lastRoll = ConcurrentHashMap<UUID, Long>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        chances[target.uniqueId] = (args.getOrNull(2)?.toDoubleOrNull() ?: 0.1).coerceIn(0.0, 1.0)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has butterfingers")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has a firm grip again")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        chances.remove(player.uniqueId)
        lastRoll.remove(player.uniqueId)
    }

    private fun roll(player: Player) {
        val chance = chances[player.uniqueId] ?: return
        val now = System.currentTimeMillis()
        if (now - (lastRoll[player.uniqueId] ?: 0L) < COOLDOWN_MILLIS)
            return
        lastRoll[player.uniqueId] = now

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
