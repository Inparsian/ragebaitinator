package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.RemoveReason
import sn.inpr.ragebaitinator.type.RbPlayer
import kotlin.math.absoluteValue

@CommandInfo(
    name = "rewind",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.RECOVERY_COMPASS,
    description = "Rewinds a player's time (specify a negative speed to move it forward)",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Rewind : BaseToggleTroll() {
    private val speed = param<Int>("Speed", "The amount to decrease the player's time by every tick.", default = 15, range = -1500..1500)

    override val syntax = withTarget(speed)

    data class RewindEntry(val originalTime: Long, val currentTime: Long, val speed: Int)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val originalOffset = target.playerTimeOffset
        val speed = args[speed]
        setField(target, "entry", RewindEntry(originalOffset, originalOffset, speed))

        setTaskTimer(target, {
            val entry = getField<RewindEntry>(target, "entry") ?: return@setTaskTimer
            val dayOffset = if (entry.currentTime <= -target.world.fullTime) 24000 else 0
            val deducted = entry.currentTime + dayOffset - entry.speed

            // player time is retained, but it takes 20 ticks to update
            // PlayOutUpdateTime packets update in the next client tick, but it's reverted after 20 ticks
            //
            // at least that's how it was for older versions, but Bukkit setPlayerTime nowadays updates the player
            // time instantly instead of in the next 20 ticks because it sends TIME_UPDATE now, so i've removed
            // the redundant packet send. if it takes a second to update it's probably on a protocol version where
            // time updates work differently
            target.setPlayerTime(deducted, true)
            setField(target, "entry", RewindEntry(entry.originalTime, deducted, speed))
        }, 0, 1)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now going %direction% in time at %c1%%speedSign%%speed% %c2%ticks/tick", target)
            .put("speed", speed.absoluteValue.toString())
            .put("speedSign", if (speed < 0) "+" else "-")
            .put("direction", if (speed < 0) "forward" else "back")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now back in the present", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        val entry = takeField<RewindEntry>(player, "entry") ?: return
        if (entry.originalTime == 0L)
            player.resetPlayerTime()
        else
            player.setPlayerTime(entry.originalTime, true)
    }
}