package sn.inpr.ragebaitinator.command.def

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.Main.Companion.runTaskRandomTimer
import sn.inpr.ragebaitinator.Main.Companion.runTaskTimer
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.type.Placeholders
import sn.inpr.ragebaitinator.utility.ChatUtil.send
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class BaseCommand {
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
                return@mapNotNull if (syntax.type == CommandSyntax.Type.PLAYER) "<player>" else null
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

    fun targetPlayers(sender: CommandSender, input: String): List<Player> = PlayerUtil.targetPlayers(input, sender)
}

abstract class BaseTroll : BaseCommand() {
    open fun getConditions(target: Player): Conditions? {
        return null
    }

    open fun init(sender: CommandSender, target: Player, args: Array<out String>): String? = null

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val targets: List<Player> = targetPlayers(sender, args.getOrNull(1) ?: return false)
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.hasPermission("ragebaitinator.immune.${this.info.name}")) {
                    val placeholders = placeholdersOr("immune", "%prefix% %c1%%player% is immune to %command%!")
                        .put("player", target.name)
                        .put("command", this.info.name)
                    send(sender, target, placeholders)
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull()
                if (firstHitCondition?.predicate == true) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% %c1%%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target, placeholders)
                    return true
                }

                val message = init(sender, target, args)
                if (message != null)
                    send(sender, target, message)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }
}

abstract class BaseToggleTroll : BaseTroll() {
    internal val players: Map<UUID, MutableSet<String>>
        field = ConcurrentHashMap()

    internal val tasks: Map<UUID, BukkitTask>
        field = mutableMapOf()

    open fun deinit(sender: CommandSender, target: Player): String? = null

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val targets: List<Player> = targetPlayers(sender, args.getOrNull(1) ?: return false)
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.hasPermission("ragebaitinator.immune.${this.info.name}")) {
                    val placeholders = placeholdersOr("immune", "%prefix% %c1%%player% is immune to %command%!")
                        .put("player", target.name)
                        .put("command", this.info.name)
                    send(sender, target, placeholders)
                    removePlayer(target, "Player is immune")
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull()
                if (firstHitCondition?.predicate == true) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% %c1%%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target, placeholders)
                    return true
                }

                for (incompatibleTroll in this.info.incompatible) {
                    if (this.info.name == incompatibleTroll)
                        continue

                    val instance = CommandManager.getCommand(incompatibleTroll)
                    if (instance is BaseToggleTroll)
                        instance.removePlayer(target, "incompatible")
                }

                val had = this.players.containsKey(target.uniqueId)
                if (!had) players[target.uniqueId] = mutableSetOf()
                val message = if (had) deinit(sender, target) else init(sender, target, args)
                if (had) removePlayer(target, "")
                if (message != null)
                    send(sender, target, message)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }

    fun addFlag(uuid: UUID, flag: String) = players[uuid]?.add(flag)
    fun addFlag(player: Player, flag: String) = addFlag(player.uniqueId, flag)
    fun hasFlag(uuid: UUID, flag: String): Boolean = players[uuid]?.contains(flag) ?: false
    fun hasFlag(player: Player, flag: String): Boolean = hasFlag(player.uniqueId, flag)

    open fun onRemovePlayer(player: Player, reason: String) {}

    fun removeTask(uuid: UUID) = tasks.remove(uuid)?.cancel()
    fun removePlayer(player: Player, reason: String): Boolean {
        val uuid = player.uniqueId
        if (players.remove(uuid) != null) {
            removeTask(uuid)
            onRemovePlayer(player, reason)
            return true
        }
        return false
    }
    fun removePlayer(player: Player): Boolean = removePlayer(player, "")

    fun clearPlayers(): Boolean {
        for (uuid in players.keys) {
            if (players.remove(uuid) == null)
                return false
            else {
                removeTask(uuid)
                val player = Bukkit.getPlayer(uuid)
                if (player != null)
                    onRemovePlayer(player, "cleared")
            }
        }
        return true
    }

    fun addTask(player: Player, runnable: Runnable, delay: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !players.contains(player.uniqueId))
            return null
        removeTask(player.uniqueId)
        val t = runTaskLater(runnable, delay)
        this.tasks[player.uniqueId] = t
        return t
    }

    fun addTaskTimer(player: Player, runnable: Runnable, delay: Long, period: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !players.contains(player.uniqueId))
            return null
        removeTask(player.uniqueId)
        val t = runTaskTimer(runnable, delay, period)
        this.tasks[player.uniqueId] = t
        return t
    }

    fun addTaskRandomTimer(player: Player, runnable: Runnable, delay: Long, minPeriod: Long, maxPeriod: Long, ignorePlayerCheck: Boolean = false): BukkitTask? {
        if (!ignorePlayerCheck && !players.contains(player.uniqueId))
            return null
        removeTask(player.uniqueId)
        val t = runTaskRandomTimer(runnable, delay, minPeriod, maxPeriod)
        this.tasks[player.uniqueId] = t
        return t
    }
}