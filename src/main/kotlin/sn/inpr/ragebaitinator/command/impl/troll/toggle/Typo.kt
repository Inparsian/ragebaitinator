package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerCommandPreprocessSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.TextTransform

@CommandInfo(
    name = "typo",
    category = CommandInfo.Category.CHAT,
    icon = Material.NAME_TAG,
    description = "Fat-fingers a player's commands. Was that /hmoe or /home?",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.DOUBLE,
            purpose = "Chance",
            description = "The chance (0 to 1) of any given command getting a typo.",
            default = "0.4"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Typo : BaseToggleTroll(), PlayerCommandPreprocessSubscriber {
    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        setField(target, "chance", args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.4)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%now has very clumsy fingers")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s fingers are no longer clumsy")
            .put("player", target.name)
            .toString()

    // never mangle our own command, or there'd be no way to turn this off
    private fun isOwnCommand(label: String): Boolean {
        val own = Main.instance().getCommand("ragebaitinator") ?: return false
        val name = label.substringAfter(':').lowercase()
        return name == own.name || name in own.aliases
    }

    override fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        if (event.isCancelled || !hasPlayer(event.player))
            return
        if (!MathUtil.randomBool(getField<Double>(event.player, "chance") ?: 0.4))
            return

        val body = event.message.removePrefix("/")
        val label = body.substringBefore(' ')
        // anything shorter is too easy to land on a different, valid command
        if (label.length < 3 || isOwnCommand(label))
            return

        event.message = "/" + TextTransform.typo(label) + body.substring(label.length)
    }
}
