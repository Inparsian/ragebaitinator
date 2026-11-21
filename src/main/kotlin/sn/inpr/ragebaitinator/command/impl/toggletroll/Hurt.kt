package sn.inpr.ragebaitinator.command.impl.toggletroll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "hurt",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.STONE_SWORD,
    description = "Repeatedly hurts a player (2 damage = 1 heart).",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Hurt : BaseToggleTroll() {
    private val damage = param<Double>("Damage", "The amount of damage to deal to the player in half-hearts.", default = 2.0, range = 0.0..PlayerUtil.MAX_POSSIBLE_DAMAGE, step = 1L)
    private val ticks = param<Long>("Ticks", "The amount of ticks between each damage.", default = 10, range = 1L..1200L, step = 1L)

    override val syntax = withTarget(damage, ticks)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): Placeholders {
        setTaskTimer(target, { target.packetDamage(args[damage]) }, 0, args[ticks])
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being damaged by %c1%%damage% %c2%every %c1%%ticks% %c2%tick(s)", target)
            .put("damage", args[damage])
            .put("ticks", args[ticks])
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): Placeholders
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer being damaged", target)
}
