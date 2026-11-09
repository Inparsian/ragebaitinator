package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPosition
import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "lost",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.COMPASS,
    description = "Makes a player's compass spaz out.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Lost : BaseToggleTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        addTaskTimer(target, {
            val x = (target.location.x + MathUtil.random(-10.0, 10.0)).toInt()
            val z = (target.location.z + MathUtil.random(-10.0, 10.0)).toInt()
            target.sendPacket(WrapperPlayServerSpawnPosition(Vector3i(x, 0, z)))
        }, 0, 1, true)
        return placeholdersOr("init", "%prefix% %c2%An unknown force is now interfering with %c1%%player%%c2%'s compass")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s compass is now back to normal")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        runTaskLater({
            val vector = player.compassTarget.toVector()
            val compassTarget = Vector3i(vector.x.toInt(), vector.y.toInt(), vector.z.toInt())
            player.sendPacket(WrapperPlayServerSpawnPosition(compassTarget))
        }, 1)
    }
}