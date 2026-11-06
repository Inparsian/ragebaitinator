package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@CommandInfo(
    name = "hypochondriac",
    description = "It's probably nothing. Every so often, a player's health bar says they're about to die.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Hypochondriac : BaseToggleTroll() {
    // everyone whose health bar is lying to them right now
    private val scared = ConcurrentHashMap.newKeySet<UUID>()

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        scheduleScare(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now think they're dying every once in a while")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has a clean bill of health")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: Player, reason: String) {
        if (scared.remove(player.uniqueId) && reason != "logout")
            showRealHealth(player)
    }

    private fun scheduleScare(player: Player) {
        addTask(player, { scare(player) }, MathUtil.random(100L, 400L))
    }

    private fun scare(player: Player) {
        // a health of 0 would put up the death screen, and there's no point if they're already hurt that badly
        if (!player.isOnline || player.isDead || player.health <= 4.0) {
            scheduleScare(player)
            return
        }

        scared.add(player.uniqueId)
        PacketEvents.getAPI().playerManager.sendPacket(
            player,
            WrapperPlayServerUpdateHealth(MathUtil.random(1f, 4f), player.foodLevel, player.saturation)
        )
        addTask(player, {
            if (scared.remove(player.uniqueId))
                showRealHealth(player)
            scheduleScare(player)
        }, MathUtil.random(30L, 80L))
    }

    private fun showRealHealth(player: Player) {
        if (player.isOnline && !player.isDead)
            PacketEvents.getAPI().playerManager.sendPacket(
                player,
                WrapperPlayServerUpdateHealth(player.health.toFloat(), player.foodLevel, player.saturation)
            )
    }
}
