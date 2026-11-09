package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "boom",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.TNT_MINECART,
    description = "Makes a player explode. Literally, that's it.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.FLOAT,
            purpose = "Power",
            description = "How powerful the explosion should be. Set to 0 for a non-destructive explosion",
            default = "0"
        )
    ]
)
class Boom : BaseTroll() {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val power = args.getOrNull(2)?.toFloatOrNull() ?: 0f
        target.world.createExplosion(target.location, power, power >= 0f)
        target.health = 0.0

        return placeholdersOr("init","%prefix% %c1%%player% %c2%has now inexplicably exploded")
            .put("player", target.name)
            .toString()
    }
}