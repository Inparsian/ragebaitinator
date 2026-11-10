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
import sn.inpr.ragebaitinator.type.asRb
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

    abstract fun process(sender: CommandSender, command: Command, label: String, args: Args): Boolean
    open fun onDisable() {}
    open fun onInstantiate() {}
    open fun onReload() {}

    val info: CommandInfo
        get() = this::class.java.getAnnotation(CommandInfo::class.java)
            ?: error("${this::class.simpleName} is missing @CommandInfo")

    /** The arguments this command takes, in the order they're typed. They are what [Args] is asked about. */
    open val syntax: List<Parameter<*>> = emptyList()

    /** An argument that has a value when it isn't given */
    protected inline fun <reified T : Any> param(
        purpose: String,
        description: String = "",
        default: T,
        completions: List<String> = emptyList(),
        strictCompletion: Boolean = false,
        completionAliases: List<String> = emptyList(),
    ): Param<T> = Param(T::class, purpose, description, true, completions, strictCompletion, completionAliases, default)

    /** A number that is moved into [range] when it's outside it */
    protected inline fun <reified T> param(
        purpose: String,
        description: String = "",
        default: T,
        range: ClosedRange<T>,
    ): Param<T> where T : Comparable<T>, T : Any =
        Param(T::class, purpose, description, true, emptyList(), false, emptyList(), default, range)

    /** An argument that is nothing when it isn't given */
    protected inline fun <reified T : Any> optional(
        purpose: String,
        description: String = "",
        completions: List<String> = emptyList(),
        strictCompletion: Boolean = false,
        completionAliases: List<String> = emptyList(),
    ): OptionalParam<T> = OptionalParam(T::class, purpose, description, completions, strictCompletion, completionAliases)

    /** An argument that has to be given */
    protected inline fun <reified T : Any> required(
        purpose: String,
        description: String = "",
        completions: List<String> = emptyList(),
        strictCompletion: Boolean = false,
        completionAliases: List<String> = emptyList(),
    ): Param<T> = Param(T::class, purpose, description, false, completions, strictCompletion, completionAliases, null)

    fun stringifySyntax(): String =
        syntax.mapNotNull { syntax ->
            if (syntax.purpose.isEmpty()) {
                return@mapNotNull if (syntax.type == Targets::class) {
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

            val inner = name + syntax.defaultText.takeIf { it.isNotEmpty() }?.let { "=$it" }.orEmpty()
            if (syntax.optional) "[$inner]" else "<$inner>"
        }.joinToString(" ")

    fun sendUsage(sender: CommandSender) {
        val placeholders = placeholdersOr("usage", "%prefix% %c2%Usage: %c3%/rb %command% %syntax%")
            .put("command", info.name)
            .put("syntax", stringifySyntax())
        send(sender, placeholders)
    }

    /** What the blocks that this command changes for a player are recorded under, so they can be put back for them alone */
    fun rollbackOwner(player: RbPlayer): String = "${info.name}:${player.uuid}"
    fun rollbackOwner(player: Player): String = rollbackOwner(player.rb)

    fun languageOr(key: String, default: String) = language.getString("commands.${info.name}.$key") ?: default

    fun placeholdersOr(key: String, default: String) = Placeholders(languageOr(key, default))
    fun placeholdersOr(key: String, default: String, player: RbPlayer) = Placeholders(languageOr(key, default))
        .put("player", player.name)

    // what a command and its parameters are described as can be changed in the language file, and are described by what
    // they were declared with otherwise
    val description: String get() = languageOr("description", info.description)
    fun parameterName(syntax: Parameter<*>): String = languageOr("parameters.${syntax.purpose}.name", syntax.purpose)
    fun parameterDescription(syntax: Parameter<*>): String = languageOr("parameters.${syntax.purpose}.description", syntax.description)

    fun targetPlayers(sender: CommandSender, input: String): List<Player> = PlayerUtil.targetPlayers(input, sender)
}

abstract class BaseTroll : BaseCommand() {
    /** The player (or players) a troll is used on. It's the first argument of every troll */
    protected val targetParam = required<Targets>("")

    override val syntax: List<Parameter<*>> = listOf(targetParam)

    /** The syntax of a troll that takes more than its target */
    protected fun withTarget(vararg parameters: Parameter<*>): List<Parameter<*>> = listOf(targetParam) + parameters

    open fun getConditions(target: RbPlayer): Conditions? = null
    fun getConditions(target: Player): Conditions? = getConditions(target.rb)

    open fun init(sender: CommandSender, target: RbPlayer, args: Args): String? = null

    override fun process(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Args
    ): Boolean {
        val playerArguments = syntax
            .filter { it.type == Targets::class }
            .map { args.input(it)?.let { input -> targetPlayers(sender, input) } ?: return false }
        val targets = playerArguments.firstOrNull()?.map { it.rb } ?: return false
        if (playerArguments.all { it.isNotEmpty() }) {
            val allTargets = playerArguments.flatten().map { it.rb }.distinctBy { it.uuid }
            for (target in allTargets) {
                if (target.isImmune(this)) {
                    val placeholders = placeholdersOr("immune", "%prefix% &c%player% is immune to %command%!", target)
                        .put("command", this.info.name)
                    send(sender, target, placeholders)
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull { it.predicate }
                if (firstHitCondition != null) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% &c%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target, placeholders)
                    return true
                }
            }

            targets.forEach { target ->
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

/**
 * This troll transforms a player into another entity.
 */
fun interface TransformsPlayer {
    fun transformation(player: RbPlayer): Entity?
}

abstract class BaseToggleTroll : BaseTroll() {
    /** Every player this troll is toggled on, along with their respective field values */
    internal val players: Map<RbPlayer, MutableMap<String, Any>>
        field = ConcurrentHashMap()

    /** The fields of a player that is being removed so that they stay readable while onRemovePlayer runs */
    private val detached = ConcurrentHashMap<RbPlayer, MutableMap<String, Any>>()

    // concurrent maps can't hold null, so this stands in for it
    private object NullValue

    private val tasks: MutableMap<RbPlayer, MutableMap<String, BukkitTask>> = mutableMapOf()

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
        args: Args
    ): Boolean {
        val targets: List<RbPlayer> = targetPlayers(sender, args.input(targetParam) ?: return false)
            .map { it.rb }
        if (targets.isNotEmpty()) {
            targets.forEach { target ->
                if (target.hasPermission("ragebaitinator.immune.${this.info.name}")) {
                    val placeholders = placeholdersOr("immune", "%prefix% &c%player% is immune to %command%!", target)
                        .put("command", this.info.name)
                    send(sender, target, placeholders)
                    removePlayer(target, RemoveReason.IMMUNE)
                    return true
                }

                val firstHitCondition = getConditions(target)?.list?.firstOrNull { it.predicate }
                if (firstHitCondition != null) {
                    val placeholders = placeholdersOr("conditionsFail", "%prefix% &c%message%")
                        .put("message", firstHitCondition.humanReadable)
                    send(sender, target, placeholders)
                    return true
                }

                for (incompatibleTroll in this.info.incompatible) {
                    if (this.info.name == incompatibleTroll)
                        continue

                    val instance = CommandManager.getCommand(incompatibleTroll)
                    if (instance is BaseToggleTroll)
                        instance.removePlayer(target, RemoveReason.INCOMPATIBLE)
                }

                val had = this.hasPlayer(target)
                if (!had) players[target] = ConcurrentHashMap()
                val message = if (had) deinit(sender, target) else init(sender, target, args)
                if (had)
                    removePlayer(target, RemoveReason.TOGGLED)
                else
                    broadcastGuiUpdate()
                if (message != null)
                    send(sender, target, message)
            }
        } else {
            send(sender, Placeholders(language.playerNotFound))
        }

        return true
    }

    fun hasPlayer(player: RbPlayer): Boolean = players.containsKey(player)
    fun hasPlayer(player: Player): Boolean = hasPlayer(player.rb)
    fun hasPlayer(uuid: UUID): Boolean = hasPlayer(RbPlayer(uuid))
    /** Whether this is a player this troll is toggled on. It's not if it isn't a player or isn't anybody */
    fun hasPlayer(entity: Entity?): Boolean = entity?.asRb?.let { hasPlayer(it) } == true

    private fun fieldsOf(player: RbPlayer): MutableMap<String, Any>? = players[player] ?: detached[player]

    fun hasField(player: RbPlayer, name: String): Boolean = fieldsOf(player)?.containsKey(name) == true
    fun hasField(player: Player, name: String): Boolean = hasField(player.rb, name)
    fun hasField(uuid: UUID, name: String): Boolean = hasField(RbPlayer(uuid), name)

    fun setField(player: RbPlayer, name: String, value: Any?) = players[player]?.put(name, value ?: NullValue)
    fun setField(player: Player, name: String, value: Any?) = setField(player.rb, name, value)
    fun setField(uuid: UUID, name: String, value: Any?) = setField(RbPlayer(uuid), name, value)

    inline fun <reified T> getField(player: RbPlayer, name: String): T? = rawField(player, name) as T?
    inline fun <reified T> getField(player: Player, name: String): T? = getField(player.rb, name)
    inline fun <reified T> getField(uuid: UUID, name: String): T? = getField(RbPlayer(uuid), name)

    inline fun <reified T> takeField(player: RbPlayer, name: String): T? = rawTakeField(player, name) as T?
    inline fun <reified T> takeField(player: Player, name: String): T? = takeField(player.rb, name)
    inline fun <reified T> takeField(uuid: UUID, name: String): T? = takeField(RbPlayer(uuid), name)

    fun removeField(player: RbPlayer, name: String): Boolean = fieldsOf(player)?.remove(name) != null
    fun removeField(uuid: UUID, name: String): Boolean = removeField(RbPlayer(uuid), name)

    inline fun <reified T> fieldEntries(name: String): Sequence<Pair<RbPlayer, T>> = rawFieldEntries(name).map { [player, value] -> player to value as T }
    inline fun <reified T> fieldValues(name: String): Sequence<T> = rawFieldEntries(name).map { it.second as T }

    @PublishedApi
    internal fun rawField(player: RbPlayer, name: String): Any? = fieldsOf(player)?.get(name)?.takeIf { it !== NullValue }

    @PublishedApi
    internal fun rawTakeField(player: RbPlayer, name: String): Any? = fieldsOf(player)?.remove(name)?.takeIf { it !== NullValue }

    @PublishedApi
    internal fun rawFieldEntries(name: String): Sequence<Pair<RbPlayer, Any>> = players.asSequence().mapNotNull { [player, fields] ->
        fields[name]?.takeIf { it !== NullValue }?.let { player to it }
    }

    // Flags are fields without a value
    fun addFlag(player: RbPlayer, flag: String) = setField(player, flag, null)
    fun addFlag(player: Player, flag: String) = setField(player, flag, null)
    fun addFlag(uuid: UUID, flag: String) = setField(uuid, flag, null)
    fun hasFlag(player: RbPlayer, flag: String): Boolean = hasField(player, flag)
    fun hasFlag(player: Player, flag: String): Boolean = hasField(player, flag)
    fun hasFlag(uuid: UUID, flag: String): Boolean = hasField(uuid, flag)
    fun removeFlag(player: RbPlayer, flag: String): Boolean = removeField(player, flag)
    fun removeFlag(uuid: UUID, flag: String): Boolean = removeField(uuid, flag)

    open fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {}

    fun hasTask(player: RbPlayer, key: String = "default"): Boolean = tasks[player]?.containsKey(key) ?: false
    fun removeTasks(player: RbPlayer): Unit? = tasks.remove(player)?.values?.forEach { it.cancel() }
    fun removeTask(player: RbPlayer, key: String = "default"): Unit? = tasks[player]?.remove(key)?.cancel()
    fun removeTask(uuid: UUID, key: String = "default"): Unit? = removeTask(RbPlayer(uuid), key)
    fun removePlayer(player: RbPlayer, reason: RemoveReason): Boolean {
        // the player stops counting as toggled before onRemovePlayer runs, which relies on that, but the fields stay
        // readable until it's done
        val fields = players.remove(player) ?: return false
        removeTasks(player)
        detached[player] = fields
        try {
            onRemovePlayer(player, reason)
        } finally {
            detached.remove(player)
        }
        broadcastGuiUpdate()
        return true
    }
    fun removePlayer(player: RbPlayer): Boolean = removePlayer(player, RemoveReason.DONE)
    fun removePlayer(player: Player, reason: RemoveReason): Boolean = removePlayer(player.rb, reason)
    fun removePlayer(player: Player): Boolean = removePlayer(player.rb)
    fun removePlayer(uuid: UUID, reason: RemoveReason): Boolean = removePlayer(RbPlayer(uuid), reason)
    fun removePlayer(uuid: UUID): Boolean = removePlayer(RbPlayer(uuid))

    fun clearPlayers(): Boolean {
        for (player in players.keys.toList())
            removePlayer(player, RemoveReason.CLEARED)
        return true
    }

    fun setTask(player: RbPlayer, key: String, runnable: Runnable, delay: Long): BukkitTask? {
        if (!hasPlayer(player))
            return null
        removeTask(player, key)
        val t = runTaskLater({
            if (hasPlayer(player))
                runnable.run()
        }, delay)
        if (this.tasks.containsKey(player))
            this.tasks[player]!![key] = t
        else
            this.tasks[player] = mutableMapOf(key to t)
        return t
    }

    fun setTask(player: RbPlayer, runnable: Runnable, delay: Long): BukkitTask? = setTask(player, "default", runnable, delay)
    fun setTask(uuid: UUID, runnable: Runnable, delay: Long): BukkitTask? = setTask(RbPlayer(uuid), "default", runnable, delay)

    fun setTaskTimer(player: RbPlayer, key: String, runnable: Runnable, delay: Long, period: Long): BukkitTask? {
        if (!hasPlayer(player))
            return null
        removeTask(player, key)
        val t = runTaskTimer({
            if (hasPlayer(player))
                runnable.run()
            else
                removeTask(player, key)
        }, delay, period)
        if (this.tasks.containsKey(player))
            this.tasks[player]!![key] = t
        else
            this.tasks[player] = mutableMapOf(key to t)
        return t
    }

    fun setTaskTimer(player: RbPlayer, runnable: Runnable, delay: Long, period: Long): BukkitTask?
        = setTaskTimer(player, "default", runnable, delay, period)

    fun setTaskRandomTimer(player: RbPlayer, key: String, runnable: Runnable, delay: Long, minPeriod: Long, maxPeriod: Long): BukkitTask? {
        if (!hasPlayer(player))
            return null
        removeTask(player, key)
        val t = runTaskRandomTimer({
            if (hasPlayer(player))
                runnable.run()
            else
                removeTask(player, key)
        }, delay, minPeriod, maxPeriod)
        if (this.tasks.containsKey(player))
            this.tasks[player]!![key] = t
        else
            this.tasks[player] = mutableMapOf(key to t)
        return t
    }

    fun setTaskRandomTimer(player: RbPlayer, runnable: Runnable, delay: Long, minPeriod: Long, maxPeriod: Long): BukkitTask?
        = setTaskRandomTimer(player, "default", runnable, delay, minPeriod, maxPeriod)
}