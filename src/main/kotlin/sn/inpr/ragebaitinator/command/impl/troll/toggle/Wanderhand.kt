package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerItemHeldEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerItemHeldSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "wanderhand",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.LEAD,
    description = "The hand has a mind of its own. Changing the selected hotbar slot sometimes skips a slot, or jumps to a random one.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Wanderhand : BaseToggleTroll(), PlayerItemHeldSubscriber {
    private val chance = param<Double>("Chance", "The chance that a slot change goes wrong (min 0, max 1).", default = 0.25, range = 0.0..1.0)

    override val syntax = withTarget(chance)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "chance", args[chance])
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s hand will now wander off by itself")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s hand stays where it's told")
            .put("player", target.name)
            .toString()

    override fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        val player = event.player
        val chance = getField<Double>(player, "chance") ?: return
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
            if (player.isOnline && hasField(player, "chance"))
                player.inventory.heldItemSlot = slot
        }
    }
}
