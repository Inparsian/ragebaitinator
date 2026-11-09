package sn.inpr.ragebaitinator.command.def

import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.PlayerUtil

data class Condition(
    val humanReadable: String,
    val key: String,
    val predicate: Boolean,
)

class Conditions {
    private val _list = mutableListOf<Condition>()
    val list: MutableList<Condition> get() = _list

    fun put(humanReadable: String, key: String, predicate: Boolean): Conditions =
        this.apply { _list.add(Condition(humanReadable, key, predicate)) }

    // boilerplate for common conditions
    fun notDead(player: RbPlayer): Conditions
        = this.put("This player must not be dead.", "dead", player.health <= 0)
    fun notInVoid(player: RbPlayer, threshold: Double = -2.0)
        = this.put("This player must not be in void.", "inVoid", PlayerUtil.inVoid(player, threshold))
}