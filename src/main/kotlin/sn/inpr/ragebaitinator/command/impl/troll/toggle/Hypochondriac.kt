package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateHealth
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "hypochondriac",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.REDSTONE,
    description = "It's probably nothing. Every so often, a player's health bar says they're about to die.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Hypochondriac : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        scheduleScare(target)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%will now think they're dying every once in a while")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has a clean bill of health")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        if (removeFlag(player, "scared") && reason != "logout")
            showRealHealth(player)
    }

    private fun scheduleScare(player: RbPlayer) {
        addTask(player, { scare(player) }, MathUtil.random(100L, 400L))
    }

    private fun scare(player: RbPlayer) {
        // a health of 0 would put up the death screen, and there's no point if they're already hurt that badly
        if (player.isDead || player.health <= 4.0) {
            scheduleScare(player)
            return
        }

        addFlag(player, "scared")
        player.sendPacket(
            WrapperPlayServerUpdateHealth(MathUtil.random(1f, 4f), player.foodLevel, player.saturation)
        )
        addTask(player, {
            if (removeFlag(player, "scared"))
                showRealHealth(player)
            scheduleScare(player)
        }, MathUtil.random(30L, 80L))
    }

    private fun showRealHealth(player: RbPlayer) {
        if (!player.isDead)
            player.sendPacket(
                WrapperPlayServerUpdateHealth(player.health.toFloat(), player.foodLevel, player.saturation)
            )
    }
}
