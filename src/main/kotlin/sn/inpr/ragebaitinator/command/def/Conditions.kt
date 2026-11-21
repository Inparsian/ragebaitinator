package sn.inpr.ragebaitinator.command.def

import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.type.RbPlayer

class Conditions(val player: RbPlayer) {
    data class Condition(private val defaultText: String, val key: String, val predicate: Boolean) {
        val text: String get() = language.stringOr("condition.$key", defaultText)
    }

    val list: List<Condition> field = mutableListOf<Condition>()

    fun condition(defaultText: String, key: String, predicate: Boolean): Conditions
        = this.apply { list.add(Condition(defaultText, key, predicate)) }

    // boilerplate for common conditions
    fun notDead() = condition("This player must not be dead.", "dead", player.health > 0)
    fun notInVoid(threshold: Double = -2.0) = condition("This player must not be in void.", "inVoid", !player.isInVoid(threshold))
}