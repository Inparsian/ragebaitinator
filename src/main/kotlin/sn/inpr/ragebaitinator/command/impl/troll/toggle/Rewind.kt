package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import java.util.UUID
import kotlin.math.absoluteValue

@CommandInfo(
    name = "rewind",
    description = "Rewinds a player's time (specify a negative speed to move it forward)",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.INTEGER,
            purpose = "Speed",
            description = "The amount to decrease the player's time by every tick.",
            default = "15"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Rewind : BaseToggleTroll() {
    data class RewindEntry(val originalTime: Long, val currentTime: Long, val speed: Int)

    private val times: MutableMap<UUID, RewindEntry> = mutableMapOf()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val originalOffset = target.playerTimeOffset
        val speed = args.getOrNull(2)?.toIntOrNull()?.coerceIn(-1500..1500) ?: 15
        times[target.uniqueId] = RewindEntry(originalOffset, originalOffset, speed)

        addTaskTimer(target, {
            val entry = times[target.uniqueId] ?: return@addTaskTimer
            val dayOffset = if (entry.currentTime <= -target.world.fullTime) 24000 else 0
            val deducted = entry.currentTime + dayOffset - entry.speed

            // player time is retained, but it takes 20 ticks to update
            // PlayOutUpdateTime packets update in the next client tick, but it's reverted after 20 ticks
            // so we use both to ensure that changes are swift and retained
            target.setPlayerTime(deducted, true)
            val packet = WrapperPlayServerTimeUpdate(target.world.time, target.playerTime)
            PacketEvents.getAPI().playerManager.sendPacket(target, packet)
            times[target.uniqueId] = RewindEntry(entry.originalTime, deducted, speed)
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now going %direction% in time at %c1%%speedSign%%speed% %c2%ticks/tick")
            .put("player", target.name)
            .put("speed", speed.absoluteValue.toString())
            .put("speedSign", if (speed < 0) "+" else "-")
            .put("direction", if (speed < 0) "forward" else "back")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is now back in the present")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        val entry = times.remove(player.uniqueId) ?: return
        if (entry.originalTime == 0L)
            player.resetPlayerTime()
        else
            player.setPlayerTime(entry.originalTime, true)
        runTaskLater({
            val packet = WrapperPlayServerTimeUpdate(player.world.time, player.playerTime)
            PacketEvents.getAPI().playerManager.sendPacket(player, packet)
        }, 1)
    }
}