package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.BlockBreakSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "boombreak",
    description = "Makes the next (or every) block a player breaks explode.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Persistent",
            description = "Should this continue to be active after the player breaks a block?",
            default = "false"
        ),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Destructive",
            description = "Should explosions be destructive?",
            default = "false"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class BoomBreak : BaseToggleTroll(), BlockBreakSubscriber {
    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val persistent = args.getOrNull(2).toBoolean()
        if (persistent)
            addFlag(target, "persistent")
        if (args.getOrNull(3).toBoolean())
            addFlag(target, "destructive")

        return "&f${if (persistent) "Every" else "The next"} block &c${target.name} &fbreaks will now explode"
    }

    override fun deinit(sender: CommandSender, target: Player): String
        = "&fThe next block &c${target.name} &fbreaks will no longer explode"

    override fun onBlockBreak(event: BlockBreakEvent) {
        if (players.contains(event.player.uniqueId)) {
            if (hasFlag(event.player, "destructive"))
                event.block.world.createExplosion(event.block.location, 5f, true)
            else
                LocationUtil.createSafeExplosion(event.block.location, 5f)

            if (event.player.gameMode != GameMode.CREATIVE && event.player.gameMode != GameMode.SPECTATOR)
                event.player.health = 0.0

            if (!hasFlag(event.player, "persistent"))
                removePlayer(event.player)
        }
    }
}