package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "randomspeed",
    category = CommandInfo.Category.MOVEMENT,
    icon = Material.SUGAR,
    description = "Gotta go fast. Or slow. I don't know.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class RandomSpeed : BaseToggleTroll() {
    private companion object {
        // the vanilla speeds are 0.2 and 0.1, and anywhere near 0 would just be a different troll
        const val MIN_WALK = 0.05f
        const val MAX_WALK = 0.35f
        const val MIN_FLY = 0.02f
        const val MAX_FLY = 0.2f
    }


    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        // the walking and flying speeds they had before
        setField(target, "originalSpeeds", target.walkSpeed to target.flySpeed)
        addTaskRandomTimer(target, { randomize(target) }, 0, 40, 160)
        return placeholdersOr("init", "%prefix% %c1%%player%%c2%'s speed will now change at random")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s speed is back to normal")
            .put("player", target.name)
            .toString()

    // the speeds are saved with the player, so they have to be put back even when they're leaving
    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        takeField<Pair<Float, Float>>(player, "originalSpeeds")?.let { [walk, fly] ->
            player.walkSpeed = walk
            player.flySpeed = fly
        }
    }

    private fun randomize(player: RbPlayer) {
        player.walkSpeed = MathUtil.random(MIN_WALK, MAX_WALK)
        player.flySpeed = MathUtil.random(MIN_FLY, MAX_FLY)
    }
}
