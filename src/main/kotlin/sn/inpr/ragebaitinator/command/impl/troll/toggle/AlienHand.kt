package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "alienhand",
    description = "MY HAND IS INFECTED BY SOME ALIEN PARASITE",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Local",
            description = "Whether this should only be visible to the player.",
            default = "true"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class AlienHand : BaseToggleTroll() {
    fun swing(target: Player, showFor: Player) {
        // dual wielding was introduced in 1.9. get if the player has an item in their offhand
        // if they do, swing their offhand too
        val offHand = PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_9)
                && target.inventory.itemInOffHand.type != Material.AIR

        val animation = if (offHand && MathUtil.randomBool())
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND
        else
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM

        val packet = WrapperPlayServerEntityAnimation(target.entityId, animation)
        PacketEvents.getAPI().playerManager.sendPacket(showFor, packet)
    }

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        if (args.getOrNull(2)?.toBooleanStrictOrNull() ?: true)
            addFlag(target, "local")

        addTaskRandomTimer(target, {
            if (hasFlag(target, "local"))
                swing(target, target)
            else
                Bukkit.getOnlinePlayers().forEach { player -> swing(target, player) }
        }, MathUtil.random(40L, 60L), 120, 160, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has Alien Hand Syndrome")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been cured of Alien Hand Syndrome")
            .put("player", target.name)
            .toString()
    }
}