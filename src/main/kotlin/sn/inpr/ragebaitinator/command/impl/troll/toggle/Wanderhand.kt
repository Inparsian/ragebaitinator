package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemHeldEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemHeldSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "wanderhand",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.LEAD,
    description = "The hand has a mind of its own. Changing the selected hotbar slot sometimes skips a slot, or jumps to a random one.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Chance",
            description = "The chance that a slot change goes wrong (min 0, max 1).",
            default = "0.25"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Wanderhand : BaseToggleTroll(), PlayerItemHeldSubscriber {
    private val chances = ConcurrentHashMap<UUID, Double>()

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        chances[target.uuid] = (args.getOrNull(2)?.toDoubleOrNull() ?: 0.25).coerceIn(0.0, 1.0)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s hand will now wander off by itself")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s hand stays where it's told")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        chances.remove(player.uuid)
    }

    override fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val chance = chances[player.uniqueId] ?: return
        if (!MathUtil.randomBool(chance))
            return

        val step = when (Math.floorMod(event.newSlot - event.previousSlot, 9)) {
            1 -> 1
            8 -> -1
            else -> 0
        }
        val slot = if (step != 0 && MathUtil.randomBool())
            Math.floorMod(event.newSlot + step, 9)
        else
            MathUtil.random(0, 9)

        if (slot == event.newSlot)
            return

        // the server applies the slot that the client asked for after this event,
        // so it can only be changed after it
        Main.runTask {
            if (player.isOnline && chances.containsKey(player.uniqueId))
                player.inventory.heldItemSlot = slot
        }
    }
}
