package sn.inpr.ragebaitinator.command.impl.troll

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "fakedamage",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.RED_DYE,
    description = "Sends fake damage to a player.",
)
class FakeDamage : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        target.packetDamage(0.0, true)
        return placeholdersOr("init", "%prefix% %c2%Sent fake damage to %c1%%player%", target)
            .toString()
    }
}