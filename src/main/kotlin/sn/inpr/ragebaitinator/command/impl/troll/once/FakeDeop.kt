package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer

@CommandInfo(
    name = "fakedeop",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.STRUCTURE_VOID,
    description = "Makes a player think they were de-opped. They won't be."
)
class FakeDeop : BaseTroll() {
    private val executor = param<String>("Executor", "The username of whoever \"de-opped\" the player. Defaults to \"Server\".", "Server")

    override val syntax = withTarget(executor)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val executor = args[executor]
        target.send(placeholdersOr("message", "&7&o[%executor%: Made %player% no longer a server operator]", target).put("executor", executor))
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%has been \"de-opped\".", target)
            .toString()
    }
}