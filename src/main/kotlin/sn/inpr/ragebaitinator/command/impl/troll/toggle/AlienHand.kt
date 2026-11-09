package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "alienhand",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.ENDERMITE_SPAWN_EGG,
    description = "MY HAND IS INFECTED BY SOME ALIEN PARASITE",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class AlienHand : BaseToggleTroll() {
    private val local = param<Boolean>("Local", "Whether this should only be visible to the player.", default = true)

    override val syntax = withTarget(local)

    fun swing(target: RbPlayer, showFor: RbPlayer) {
        // dual wielding was introduced in 1.9. get if the player has an item in their offhand
        // if they do, swing their offhand too
        val offHand = PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_9)
                && target.itemInOffHand.type != Material.AIR

        val animation = if (offHand && MathUtil.randomBool())
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND
        else
            WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM

        showFor.sendPacket(WrapperPlayServerEntityAnimation(target.entityId, animation))
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[local]) addFlag(target, "local")

        addTaskRandomTimer(target, {
            if (hasFlag(target, "local"))
                swing(target, target)
            else
                Bukkit.getOnlinePlayers().forEach { player -> swing(target, player.rb) }
        }, MathUtil.random(40L, 60L), 120, 160, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has Alien Hand Syndrome")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been cured of Alien Hand Syndrome")
            .put("player", target.name)
            .toString()
    }
}