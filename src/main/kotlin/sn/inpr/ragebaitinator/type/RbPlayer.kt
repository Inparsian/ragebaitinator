package sn.inpr.ragebaitinator.type

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

/**
 * Shorthand for converting a Bukkit player to a Ragebaitinator player
 */
val Player.rb: RbPlayer get() = RbPlayer(this)

/**
 * A Ragebaitinator player
 */
class RbPlayer(player: Player) {
    companion object {
        val online: List<RbPlayer> = Bukkit.getOnlinePlayers().map { it.rb }
    }

    private val cachedBukkit: Player = player
    val bukkit: Player get() {
        if (cachedBukkit.isOnline) {
            return cachedBukkit
        }
        return Bukkit.getPlayer(cachedBukkit.uniqueId) ?: cachedBukkit
    }

    val others: List<RbPlayer> get() = online.filter { it != this }

    val uuid: UUID get() = bukkit.uniqueId
    val name: String get() = bukkit.name

    override fun equals(other: Any?) = other is RbPlayer && other.uuid == uuid
    override fun hashCode() = uuid.hashCode()
    override fun toString() = "RbPlayer(${name})"
}