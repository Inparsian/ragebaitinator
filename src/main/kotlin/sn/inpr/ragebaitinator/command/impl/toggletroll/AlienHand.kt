package sn.inpr.ragebaitinator.command.impl.toggletroll

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemSwingAnimation
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSwingAnimation
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rbPlayers
import sn.inpr.ragebaitinator.type.sendPacket
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
    private val frequency = param<Int>("Frequency", "How frequently to swing the player's hand, in ticks.", default = 140, range = 5..360)

    override val syntax = withTarget(frequency)

    fun swing(target: RbPlayer) {
        // dual wielding was introduced in 1.9. get if the player has an item in their offhand
        // if they do, swing their offhand too
        val offHand = PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_9)
                && target.itemInOffHand.type != Material.AIR
                && MathUtil.randomBool()

        val packet = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_26_3)) {
            val animation = if (offHand) EntityAnimationType.SWING_OFF_HAND else EntityAnimationType.SWING_MAIN_ARM
            WrapperPlayServerEntityAnimation(target.entityId, animation)
        } else {
            val hand = if (offHand) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
            WrapperPlayServerSwingAnimation(target.entityId, hand, ItemSwingAnimation(ItemSwingAnimation.Type.WHACK, 6))
        }

        target.world.rbPlayers.sendPacket(packet)
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val minPeriod = (args[frequency].toFloat() / 1.25).toLong()
        val maxPeriod = (args[frequency].toFloat() * 1.25).toLong()
            setTaskRandomTimer(target, {
            swing(target)
        }, MathUtil.random(minPeriod, maxPeriod), minPeriod, maxPeriod)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has Alien Hand Syndrome", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been cured of Alien Hand Syndrome", target)
            .toString()
    }
}