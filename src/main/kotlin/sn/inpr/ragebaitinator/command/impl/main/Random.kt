package sn.inpr.ragebaitinator.command.impl.main

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.ChatUtil.send

@CommandInfo(
    name = "random",
    description = "Shows the usage of a random troll. Give a player to run one on them instead.",
)
class Random : BaseCommand() {
    private val target = optional<Targets>("")

    override val syntax = listOf(target)

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        val allowed = CommandManager.trolls.filter { sender.hasPermission("ragebaitinator.commands.${it.info.name.lowercase()}") }

        if (args.input(target) == null) {
            val troll = allowed.randomOrNull()
            if (troll == null) {
                send(sender, Placeholders("commands.random.none", "%prefix% &cThere are no trolls you can use"))
                return true
            }

            val placeholders = placeholdersOr("suggestion", "%prefix% %c2%How about %c1%%troll%%c2%? %c3%%description%")
                .put("troll", troll.info.name)
                .put("description", troll.description)
            send(sender, placeholders)
            troll.sendUsage(sender)
            return true
        }

        val targets = args.getOrNull(target)
        if (targets == null) {
            send(sender, Placeholders(language.playerNotFound))
            return true
        }

        val troll = allowed.filter { runnable(it, targets) }.randomOrNull()
        if (troll == null) {
            send(sender, Placeholders("commands.random.nothingToRun", "%prefix% &cThere is no troll that can be run on them without more arguments"))
            return true
        }

        val placeholders = placeholdersOr("running", "%prefix% %c2%Running %c1%%troll%")
            .put("troll", troll.info.name)
        send(sender, placeholders)
        Bukkit.dispatchCommand(sender, "$label ${troll.info.name} ${args.input(target)}")
        return true
    }

    // everything but the target has to be optional, and it has to work on everyone
    private fun runnable(troll: BaseTroll, targets: List<RbPlayer>): Boolean {
        return !troll.syntax.any { !it.optional && it.type != Targets::class } && targets.none { player ->
            player.isImmune(troll)
                    || (troll is BaseToggleTroll && troll.hasPlayer(player))
                    || troll.getConditions(player)?.list?.any { it.predicate } == true
        }
    }
}
