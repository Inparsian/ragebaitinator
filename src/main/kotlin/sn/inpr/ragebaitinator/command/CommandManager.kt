package sn.inpr.ragebaitinator.command

import io.github.classgraph.ClassGraph
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.database.CommandHistory
import sn.inpr.ragebaitinator.gui.CommandHistorySubscriber
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.LogUtil.info
import java.util.*
import java.util.stream.Collectors

object CommandManager : CommandExecutor, TabCompleter {
    val commands: List<BaseCommand>
        field = mutableListOf()

    // These are derived from `commands`, which only changes in registerCommands, so they are built
    // once there instead of on every access
    // RbPlayer.entity reads `transformers` on nearly every call, from any thread
    @Volatile var trolls: List<BaseTroll> = emptyList()
        private set
    @Volatile var toggleTrolls: List<BaseToggleTroll> = emptyList()
        private set
    @Volatile var onceTrolls: List<BaseTroll> = emptyList()
        private set
    /** The toggle trolls that can transform a player into another entity */
    @Volatile var transformers: List<TransformsPlayer> = emptyList()
        private set

    private fun refreshCaches() {
        trolls = commands.filterIsInstance<BaseTroll>()
        toggleTrolls = commands.filterIsInstance<BaseToggleTroll>()
        onceTrolls = trolls.filterNot { it is BaseToggleTroll }
        transformers = toggleTrolls.filterIsInstance<TransformsPlayer>()
    }

    fun registerCommands() {
        commands.clear()
        refreshCaches()
        ClassGraph()
            .acceptPackages("sn.inpr.ragebaitinator.command.impl")
            .enableClassInfo()
            .enableAnnotationInfo() // required for @CommandInfo
            .scan()
            .use { scan ->
                scan.getSubclasses(BaseCommand::class.java)
                    .filter { !it.isAbstract }
                    .loadClasses()
                    .map { it.getDeclaredConstructor().newInstance() as BaseCommand }
                    .filter { it::class.java.isAnnotationPresent(CommandInfo::class.java) }
                    .forEach { cmd ->
                        commands.add(cmd)
                        cmd.onInstantiate()
                    }
            }

        refreshCaches()
        info("Registered ${commands.size} commands")
    }

    fun getCommand(name: String?): BaseCommand? {
        return if (name != null) commands.firstOrNull { it.info.name.equals(name, ignoreCase = true) } else null
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && commands.any { it.info.name == args.first() }) {
            val first = commands.first { it.info.name == args.first() }
            if (!first.process(sender, command, label, args))
                first.sendUsage(sender)
            else {
                val everything = "${command.label} ${args.joinToString(" ")}"
                if (sender is Player)
                    CommandHistory.addCommand(sender.name, everything)
                else
                    CommandHistory.addConsoleCommand(everything)

                GuiHandler.menus.values
                    .filter { it is CommandHistorySubscriber }
                    .forEach { it.update() }
            }
        } else {
            send(sender, Placeholders("commandNotFound","%prefix% &cCommand not found"))
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val list: MutableList<String> = mutableListOf()

        if (args.size == 1) {
            list.addAll(commands.map { it.info.name })
        } else if (args.size > 1) {
            val command: BaseCommand? = getCommand(args[0])
            val syntax: Array<CommandSyntax>? = command?.info?.syntax

            if (syntax != null) {
                for (i in syntax.indices) {
                    if (i != args.size - 2)
                        continue

                    val syntaxArg: CommandSyntax = syntax[i]
                    when (syntaxArg.type) {
                        CommandSyntax.Type.PLAYER -> list.addAll(Bukkit.getOnlinePlayers().map { it.name })
                        CommandSyntax.Type.BOOLEAN -> list.addAll(listOf("true", "false"))
                        CommandSyntax.Type.STRING -> list.addAll(syntaxArg.completions)
                        else -> {}
                    }
                    break
                }
            }
        }

        return if (list.isNotEmpty())
            list.stream()
                .filter { s: String? -> s!!.lowercase(Locale.getDefault()).startsWith(args.last()) }
                .collect(Collectors.toList())
        else
            mutableListOf()
    }
}