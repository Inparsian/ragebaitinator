package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerExpChangeEvent
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.listeners.PlayerExpChangeSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "taxman",
    category = CommandInfo.Category.SURVIVAL,
    icon = Material.EXPERIENCE_BOTTLE,
    description = "Death and taxes. Takes a cut out of every XP orb a player collects.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Taxman : BaseToggleTroll(), PlayerExpChangeSubscriber {
    private val rate = param<Double>("Rate", "The fraction (0 to 1) of every XP gain that gets taxed.", default = 0.5, range = 0.0..1.0)

    override val syntax = withTarget(rate)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val rate = args[rate]
        setField(target, "rate", rate)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now pays %c1%%rate% %c2%income tax on all XP")
            .put("player", target.name)
            .put("rate", "${(rate * 100).toInt()}%")
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%has been audited and no longer pays XP tax")
            .put("player", target.name)
            .toString()

    override fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        if (!hasPlayer(event.player) || event.amount <= 0)
            return

        // the house always wins, so this rounds down what the player gets to keep
        event.amount = (event.amount * (1.0 - (getField<Double>(event.player, "rate") ?: 0.5))).toInt()
    }
}
