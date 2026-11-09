package sn.inpr.ragebaitinator.command.def

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.scheduler.BukkitTask
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.Main.Companion.runTaskRandomTimer
import sn.inpr.ragebaitinator.Main.Companion.runTaskTimer
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.gui.GuiHandler
import sn.inpr.ragebaitinator.gui.TrollToggleSubscriber
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class BaseCommand {
    fun Event.cancelIf(predicate: Boolean) {
        if (this is Cancellable && predicate)
            isCancelled = true
    }

    fun Event.cancelIfAndThen(predicate: Boolean, runnable: Runnable) {
        if (this is Cancellable && predicate) {
            isCancelled = true
            runnable.run()
        }
    }

    abstract fun process(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean
    open fun onDisable() {}
    open fun onInstantiate() {}
    open fun onReload() {}

    val info: CommandInfo
        get() = this::class.java.getAnnotation(CommandInfo::class.java)
            ?: error("${this::class.simpleName} is missing @CommandInfo")

    fun stringifySyntax(): String =
        info.syntax.mapNotNull { syntax ->
            if (syntax.purpose.isEmpty()) {
                return@mapNotNull if (syntax.type == CommandSyntax.Type.PLAYER) {
                    if (syntax.optional) "[player]" else "<player>"
                } else null
            }

            val name = syntax.purpose
                .split(" ")
                .filter { it.any { c -> !c.isDigit() } }
                .mapIndexed { i, part ->
                    if (i == 0) part.lowercase()
                    else part.replaceFirstChar { it.uppercaseChar() }.lowercase().let {
                        part.replaceFirstChar { it.titlecase() }
                    }
                }
                .joinToString("")

            val inner = name + syntax.default.takeIf { it.isNotEmpty() }?.let { "=$it" }.orEmpty()
            if (syntax.optional) "[$inner]" else "<$inner>"
        }.joinToString(" ")

    fun sendUsage(sender: CommandSender) {
        val placeholders = placeholdersOr("usage", "%prefix% %c2%Usage: %c3%/rb %command% %syntax%")
            .put("command", info.name)
            .put("syntax", stringifySyntax())
        send(sender, placeholders)
    }

    fun languageOr(key: String, default: String) = language.getString("commands.${info.name}.$key") ?: default

    fun placeholdersOr(key: String, default: String) = Placeholders(languageOr(key, default))

    // what a command and its parameters are described as can be changed in the language file, and are described by their
    // annotations otherwise
    val description: String get() = languageOr("description", info.description)
    fun parameterName(syntax: CommandSyntax): String = languageOr("parameters.${syntax.purpose}.name", syntax.purpose)
    fun parameterDescription(syntax: CommandSyntax): String = languageOr("parameters.${syntax.purpose}.description", syntax.description)

    fun targetPlayers(sender: CommandSender, input: String): List<Player> = PlayerUtil.targetPlayers(input, sender)
}

abstract class BaseTroll : BaseCommand() {
    open fun getConditions(target: RbPlayer): Conditions? = null
    fun getConditions(target: Player): Conditions? = getConditions(target.rb)

    open fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String? = null

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val playerArguments = info.syntax.indices
            .filter { info.syntax[it].type == CommandSyntax.Type.PLAYER }
            .map { args.getOrNull(it + 1)?.let { input -> targetPlayers(sender, input) } ?: return false }
        val targets = playerArguments.firstOrNull()?.map { it.rb } ?: return false
        if (playerArguments.all { it.isNotEmpty() }) {
            val allTargets = playerArguments.flatten().map { it.rb }.distinctBy { it.uuid }
            for (target in allTargets) {
                if (target.isImmune(this)) {
                    val placeholders = placeholdersOr("immune", "%prefix% &c%player% is immune to %command%!")
                        .put("player", target.name)
                        .put("command", this.info.name)
                    send(sender, target.bukkit, placeholders)
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull { it.predicate }
                if (firstHitCondition != null) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% &c%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target.bukkit, placeholders)
                    return true
                }
            }

            targets.forEach { target ->
                val message = init(sender, target, args)
                if (message != null)
                    send(sender, target.bukkit, message)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }
}

/**
 * This troll transforms a player into another entity.
 */
fun interface TransformsPlayer {
    fun transformation(player: RbPlayer): Entity?
}

abstract class BaseToggleTroll : BaseTroll() {
    internal val players: Map<RbPlayer, MutableSet<String>>
        field = ConcurrentHashMap()

    internal val tasks: Map<RbPlayer, BukkitTask>
        field = mutableMapOf()

    fun broadcastGuiUpdate() {
        GuiHandler.menus.values
            .filter { it is TrollToggleSubscriber }
            .forEach { it.update() }
    }

    open fun deinit(sender: CommandSender, target: RbPlayer): String? = null

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val targets: List<RbPlayer> = targetPlayers(sender, args.getOrNull(1) ?: return false)
            .map { it.rb }
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.hasPermission("ragebaitinator.immune.${this.info.name}")) {
                    val placeholders = placeholdersOr("immune", "%prefix% &c%player% is immune to %command%!")
                        .put("player", target.name)
                        .put("command", this.info.name)
                    send(sender, target.bukkit, placeholders)
                    removePlayer(target, "immune")
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull { it.predicate }
                if (firstHitCondition != null) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% &c%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target.bukkit, placeholders)
                    return true
                }

                for (incompatibleTroll in this.info.incompatible) {
                    if (this.info.name == incompatibleTroll)
                        continue

                    val instance = CommandManager.getCommand(incompatibleTroll)
                    if (instance is BaseToggleTroll)
                        instance.removePlayer(target, "incompatible")
                }

                val had = this.hasPlayer(target)
                if (!had) players[target] = mutableSetOf()
                val message = if (had) deinit(sender, target) else init(sender, target, args)
                if (had)
                    removePlayer(target, "")
                else
                    broadcastGuiUpdate()
                if (message != null)
                    send(sender, target.bukkit, message)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }

    fun hasPlayer(player: RbPlayer): Boolean = players.containsKey(player)
    fun hasPlayer(player: Player): Boolean = hasPlayer(player.rb)
    fun hasPlayer(uuid: UUID): Boolean = hasPlayer(RbPlayer(uuid))

    fun addFlag(player: RbPlayer, flag: String): Boolean? = players[player]?.add(flag)
    fun addFlag(uuid: UUID, flag: String): Boolean? = addFlag(RbPlayer(uuid), flag)
    fun hasFlag(player: RbPlayer, flag: String): Boolean = players[player]?.contains(flag) ?: false
    fun hasFlag(player: Player, flag: String): Boolean = hasFlag(player.rb, flag)
    fun hasFlag(uuid: UUID, flag: String): Boolean = hasFlag(RbPlayer(uuid), flag)
    fun removeFlag(player: RbPlayer, flag: String): Boolean = players[player]?.remove(flag) ?: false
    fun removeFlag(uuid: UUID, flag: String): Boolean = removeFlag(RbPlayer(uuid), flag)

    open fun onRemovePlayer(player: RbPlayer, reason: String) {}

    fun removeTask(player: RbPlayer): Unit? = tasks.remove(player)?.cancel()
    fun removeTask(uuid: UUID): Unit? = removeTask(RbPlayer(uuid))
    fun removePlayer(player: RbPlayer, reason: String): Boolean {
        if (players.remove(player) != null) {
            removeTask(player)
            onRemovePlayer(player, reason)
            broadcastGuiUpdate()
            return true
        }
        return false
    }
    fun removePlayer(player: RbPlayer): Boolean = removePlayer(player, "")
    fun removePlayer(player: Player, reason: String): Boolean = removePlayer(player.rb, reason)
    fun removePlayer(player: Player): Boolean = removePlayer(player.rb)
    fun removePlayer(uuid: UUID, reason: String): Boolean = removePlayer(RbPlayer(uuid), reason)
    fun removePlayer(uuid: UUID): Boolean = removePlayer(RbPlayer(uuid))

    fun clearPlayers(): Boolean {
        for (player in players.keys) {
            if (players.remove(player) == null)
                return false
            else {
                removeTask(player)
                broadcastGuiUpdate()
                onRemovePlayer(player, "cleared")
            }
        }
        return true
    }

    fun addTask(player: RbPlayer, runnable: Runnable, delay: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !hasPlayer(player))
            return null
        removeTask(player)
        val t = runTaskLater(runnable, delay)
        this.tasks[player] = t
        return t
    }

    fun addTask(uuid: UUID, runnable: Runnable, delay: Long): BukkitTask? {
        return addTask(RbPlayer(uuid), runnable, delay)
    }

    fun addTaskTimer(player: RbPlayer, runnable: Runnable, delay: Long, period: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !hasPlayer(player))
            return null
        removeTask(player)
        val t = runTaskTimer(runnable, delay, period)
        this.tasks[player] = t
        return t
    }

    fun addTaskRandomTimer(player: RbPlayer, runnable: Runnable, delay: Long, minPeriod: Long, maxPeriod: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !hasPlayer(player))
            return null
        removeTask(player)
        val t = runTaskRandomTimer(runnable, delay, minPeriod, maxPeriod)
        this.tasks[player] = t
        return t
    }
}