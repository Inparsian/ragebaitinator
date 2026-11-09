package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PacketEventSendSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*

@CommandInfo(
    name = "storm",
    category = CommandInfo.Category.WORLD,
    icon = Material.LIGHTNING_ROD,
    description = "The perfect weather for sulking.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Interval",
            description = "About how many seconds are between lightning flashes (min 1, max 60). Set to 0 for no lightning.",
            default = "8"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Storm : BaseToggleTroll(), PacketEventSendSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val seconds = (args.getOrNull(2)?.toDoubleOrNull() ?: 8.0)
        brew(target)
        if (seconds > 0) {
            val interval = (seconds.coerceIn(1.0, 60.0) * 20).toLong()
            addTaskRandomTimer(target, { flash(target) }, interval / 2, interval / 2, interval * 3 / 2)
        }
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now in the middle of a storm")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s sky has cleared up")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        if (player.isOnline && reason != "logout")
            clear(player)
    }

    private fun send(player: RbPlayer, reason: WrapperPlayServerChangeGameState.Reason, value: Float) {
        // silently, because our own listener cancels weather for everyone with a storm
        player.sendPacketSilently(WrapperPlayServerChangeGameState(reason, value))
    }

    private fun brew(player: RbPlayer) {
        if (!player.isOnline || player.world.environment != World.Environment.NORMAL)
            return

        send(player, WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING, 0f)
        send(player, WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE, 1f)
        send(player, WrapperPlayServerChangeGameState.Reason.THUNDER_LEVEL_CHANGE, 1f)
    }

    private fun clear(player: RbPlayer) {
        val world = player.world
        if (world.hasStorm()) {
            send(player, WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING, 0f)
            send(player, WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE, 1f)
        } else {
            send(player, WrapperPlayServerChangeGameState.Reason.END_RAINING, 0f)
            send(player, WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE, 0f)
        }
        send(player, WrapperPlayServerChangeGameState.Reason.THUNDER_LEVEL_CHANGE, if (world.isThundering) 1f else 0f)
    }

    private fun flash(player: RbPlayer) {
        if (player.isDead)
            return

        if (player.world.environment != World.Environment.NORMAL)
            return

        val angle = MathUtil.random(0.0, 360.0)
        val distance = MathUtil.random(12.0, 48.0)
        val x = player.location.x + MathUtil.rcos(angle) * distance
        val z = player.location.z + MathUtil.rsin(angle) * distance
        val y = player.world.getHighestBlockYAt(x.toInt(), z.toInt()) + 1.0

        player.sendPacket(WrapperPlayServerSpawnEntity(
            SpigotReflectionUtil.generateEntityId(), UUID.randomUUID(), EntityTypes.LIGHTNING_BOLT,
            Location(x, y, z, 0f, 0f), 0f, 0, Vector3d.zero()
        ))
    }

    // the real weather changing (or being sent again when a player joins or changes worlds) must not clear the storm
    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        if (!hasPlayer(uuid))
            return

        // switching worlds makes a new level for the client, which starts out clear
        if (event.packetType == PacketType.Play.Server.RESPAWN) {
            Main.runTaskLater({
                val player = Bukkit.getPlayer(uuid)
                if (player != null && hasPlayer(uuid)) brew(player.rb)
            }, 2)
            return
        }
        if (event.packetType != PacketType.Play.Server.CHANGE_GAME_STATE)
            return

        when (WrapperPlayServerChangeGameState(event).reason) {
            WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING,
            WrapperPlayServerChangeGameState.Reason.END_RAINING,
            WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE,
            WrapperPlayServerChangeGameState.Reason.THUNDER_LEVEL_CHANGE -> event.isCancelled = true
            else -> {}
        }
    }
}
