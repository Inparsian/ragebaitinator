package sn.inpr.ragebaitinator.command.def

import org.bukkit.permissions.Permissible
import sn.inpr.ragebaitinator.utility.PlayerUtil.targetPlayers

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandSyntax(
    val type: Type,
    val strictCompletion: Boolean = false,
    val completions: Array<String> = [],
    // these are not in tab-completion and are merely counted towards strict completion
    val completionAliases: Array<String> = [],
    val purpose: String = "",
    val description: String = "",
    val optional: Boolean = true,
    val default: String = "",
) {
    enum class Type(private val tName: String) {
        STRING("String"),
        BOOLEAN("Boolean"),
        INTEGER("Integer"),
        LONG("Long"),
        FLOAT("Float"),
        DOUBLE("Double"),
        PLAYER("Player");

        override fun toString(): String {
            return tName
        }

        fun isValid(input: String, whoAsked: Permissible? = null): Boolean = when (this) {
            STRING -> true // well duh of course
            BOOLEAN, INTEGER, LONG, FLOAT, DOUBLE -> when (this) {
                BOOLEAN -> input.toBooleanStrictOrNull()
                INTEGER -> input.toIntOrNull()
                LONG -> input.toLongOrNull()
                FLOAT -> input.toFloatOrNull()
                DOUBLE -> input.toDoubleOrNull()
            } != null
            // we only care if targetPlayers actually returned anything
            // if it did, there's at least one player that matches our query
            PLAYER -> targetPlayers(input, whoAsked).isNotEmpty()
        }
    }
}
