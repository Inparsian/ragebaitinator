package sn.inpr.ragebaitinator.command.def

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.permissions.Permissible
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.PlayerUtil
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/** The players an argument selects, like a name, `@a` or `world=...`. There is always at least one */
class Targets(private val players: List<RbPlayer>) : AbstractList<RbPlayer>() {
    override val size: Int get() = players.size
    override fun get(index: Int): RbPlayer = players[index]
}

/** The name of a player who might not be online (or `!!` for the console), exactly as it was typed */
data class PlayerName(val value: String)

/**
 * Implemented by an enum whose constants can be typed under other names as well. Those names are accepted but are
 * never suggested or listed, and no two constants may have the same one
 */
interface Aliased {
    val aliases: List<String>
}

/**
 * How the text of an argument turns into a value. This is everything that is known about a type of argument, so adding
 * one means adding it to [ArgumentTypes]
 */
interface ArgumentType<T : Any> {
    val type: KClass<T>
    /** What the type is called in the language file, under `gui.types` */
    val key: String
    fun parse(input: String, source: Permissible?): T?
    fun format(value: T): String = value.toString()
    fun completions(): List<String> = emptyList()
    /** For a type that only has so many values, all of them. Nothing else is a value, so these are also what is listed */
    val values: List<String> get() = emptyList()
    /** Other ways of writing the values. These are accepted, but nothing suggests them */
    val aliases: List<String> get() = emptyList()
}

object ArgumentTypes {
    private class Simple<T : Any>(
        override val type: KClass<T>,
        override val key: String,
        private val parser: (String) -> T?,
        private val formatter: (T) -> String = { it.toString() },
        private val completer: () -> List<String> = { emptyList() },
    ) : ArgumentType<T> {
        override fun parse(input: String, source: Permissible?): T? = parser(input)
        override fun format(value: T): String = formatter(value)
        override fun completions(): List<String> = completer()
    }

    private fun onlinePlayers(): List<String> = Bukkit.getOnlinePlayers().map { it.name }

    // 2.0 is typed as 2
    private fun number(value: Any): String = value.toString().removeSuffix(".0")

    private val registry: Map<KClass<*>, ArgumentType<*>> = listOf(
        Simple(String::class, "string", { it }),
        Simple(Boolean::class, "boolean", { it.toBooleanStrictOrNull() }, completer = { listOf("true", "false") }),
        Simple(Int::class, "integer", { it.toIntOrNull() }),
        Simple(Long::class, "long", { it.toLongOrNull() }),
        Simple(Float::class, "float", { it.toFloatOrNull() }, ::number),
        Simple(Double::class, "double", { it.toDoubleOrNull() }, ::number),
        TargetType(),
        Simple(PlayerName::class, "player", { PlayerName(it) }, { it.value }, ::onlinePlayers),
    ).associateBy { it.type }

    // we only care if targetPlayers actually returned anything. if it did, at least one player matches the query
    private class TargetType : ArgumentType<Targets> {
        override val type = Targets::class
        override val key = "player"
        override fun parse(input: String, source: Permissible?): Targets? =
            PlayerUtil.targetPlayers(input, source).map { it.rb }.takeIf { it.isNotEmpty() }?.let { Targets(it) }
        override fun completions(): List<String> = onlinePlayers()
    }

    /** Any enum is a type of argument, as the constant that was typed. How it's capitalized doesn't matter */
    private class EnumType<E : Enum<*>>(override val type: KClass<E>) : ArgumentType<E> {
        private val constants: List<E> = type.java.enumConstants.toList()
        private val aliasesOf: Map<E, List<String>> = constants.associateWith { (it as? Aliased)?.aliases.orEmpty() }
        override val key = "enum"
        override val values: List<String> = constants.map { it.name.lowercase() }
        override val aliases: List<String> = aliasesOf.values.flatten()

        init {
            // two constants that can be typed the same way would leave the second one unreachable
            val taken = (values + aliases).groupingBy { it.lowercase() }.eachCount().filterValues { it > 1 }.keys
            check(taken.isEmpty()) { "${type.simpleName} has constants that can be typed the same way: $taken" }
        }

        override fun parse(input: String, source: Permissible?): E? =
            constants.firstOrNull { it.name.equals(input, true) || aliasesOf.getValue(it).any { alias -> alias.equals(input, true) } }
        override fun format(value: E): String = value.name.lowercase()
        override fun completions(): List<String> = values
    }

    private val enumTypes = ConcurrentHashMap<KClass<*>, ArgumentType<*>>()

    @Suppress("UNCHECKED_CAST")
    fun of(type: KClass<*>): ArgumentType<*> =
        registry[type] ?: registry[type.javaObjectType.kotlin]
            ?: type.takeIf { it.java.isEnum }?.let { enumTypes.getOrPut(it) { EnumType(it as KClass<Enum<*>>) } }
            ?: error("There is no type of argument for ${type.simpleName}")
}

/**
 * One argument of a command. Example:
 *
 *     private val power = param<Double>("Power", "How hard.", default = 2.0, range = 0.0..5.0)
 *     override val syntax = withTarget(power)
 *     ...
 *     val power = args[power]
 *
 * A [Param] always has a value, and an [OptionalParam] might not.
 */
sealed class Parameter<T : Any>(
    val type: KClass<T>,
    /** What this is called. The target of a troll is the only one without one */
    val purpose: String,
    val description: String,
    val optional: Boolean,
    val completions: List<String>,
    /** Whether only the completions (and their aliases) are accepted */
    val strictCompletion: Boolean,
    // these are not in tab-completion and are merely counted towards strict completion
    val completionAliases: List<String>,
) {
    @Suppress("UNCHECKED_CAST")
    val argumentType: ArgumentType<T> get() = ArgumentTypes.of(type) as ArgumentType<T>

    abstract val defaultText: String

    /** What to tell somebody the choices are: what it was declared with, or else all there are */
    val possibleValues: List<String> get() = completions.ifEmpty { argumentType.values }

    fun parse(input: String, source: Permissible?): T? = argumentType.parse(input, source)?.let { constrain(it) }

    protected open fun constrain(value: T): T = value
}

class Param<T : Any>(
    type: KClass<T>,
    purpose: String,
    description: String,
    optional: Boolean,
    completions: List<String>,
    strictCompletion: Boolean,
    completionAliases: List<String>,
    val default: T?,
    val range: ClosedRange<*>? = null,
) : Parameter<T>(type, purpose, description, optional, completions, strictCompletion, completionAliases) {
    override val defaultText: String get() = default?.let { argumentType.format(it) } ?: ""

    @Suppress("UNCHECKED_CAST")
    override fun constrain(value: T): T {
        val range = range as? ClosedRange<Comparable<Any>>? ?: return value
        val comparable = value as Comparable<Any>
        return when {
            comparable < range.start -> range.start as T
            comparable > range.endInclusive -> range.endInclusive as T
            else -> value
        }
    }
}

class OptionalParam<T : Any>(
    type: KClass<T>,
    purpose: String,
    description: String,
    completions: List<String>,
    strictCompletion: Boolean,
    completionAliases: List<String>,
) : Parameter<T>(type, purpose, description, true, completions, strictCompletion, completionAliases) {
    override val defaultText: String get() = ""
}

/** What was typed after the name of a command. It's handed what it needs to know which text belongs to which parameter */
class Args(val raw: Array<out String>, private val command: BaseCommand, private val sender: CommandSender) {
    val size: Int get() = raw.size

    fun input(parameter: Parameter<*>): String? {
        val index = command.syntax.indexOf(parameter)
        check(index >= 0) { "'${parameter.purpose}' is not part of the syntax of ${command.info.name}" }
        return raw.getOrNull(index + 1)?.takeIf { it.isNotEmpty() }
    }

    fun <T : Any> getOrNull(parameter: Parameter<T>): T? =
        input(parameter)?.let { parameter.parse(it, sender) }

    operator fun <T : Any> get(parameter: Param<T>): T =
        getOrNull(parameter)
            ?: parameter.default
            ?: error("'${parameter.purpose}' of ${command.info.name} has to be given")

    operator fun <T : Any> get(parameter: OptionalParam<T>): T? = getOrNull(parameter)
}
