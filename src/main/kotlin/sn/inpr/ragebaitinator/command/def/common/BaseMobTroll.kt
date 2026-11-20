package sn.inpr.ragebaitinator.command.def.common

import com.github.retrooper.packetevents.protocol.entity.type.EntityType
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import org.bukkit.Location
import org.bukkit.entity.Creature
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.Param
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb

/** A toggle troll that works with mobs in some way */
abstract class BaseMobTroll : BaseToggleTroll() {
    protected companion object {
        const val LAST_SHARED_INDEX = 15
        const val MC_NAMESPACE = "minecraft:"

        fun mobParam(purpose: String, description: String, default: String = "creeper")
            = Param(String::class, purpose, description, true, getMobsAsStrings(), true, emptyList(), default)

        // a lookup by name wants the namespace in some versions of PacketEvents, and not in others
        fun resolve(name: String): EntityType? {
            val name = sanitize(name)
            return listOf("$MC_NAMESPACE$name", name).firstNotNullOfOrNull { runCatching { EntityTypes.getByName(it) }.getOrNull() }
        }

        fun getMobsAsStrings(): List<String>
            = EntityTypes.values().filter { EntityTypes.isTypeInstanceOf(it, EntityTypes.ABSTRACT_INSENTIENT) }.map { it.name.key }
                .filter { !it.startsWith("abstract_") }

        fun isMob(type: EntityType): Boolean = EntityTypes.isTypeInstanceOf(type, EntityTypes.ABSTRACT_INSENTIENT)

        fun sanitize(input: String) = input.lowercase().removePrefix(MC_NAMESPACE)
    }
}

/** A toggle troll that spawns a mob targeting the player whenever something happens. There's a flag for persistence. */
abstract class BaseSpawnMobTroll : BaseMobTroll() {
    fun addFields(target: RbPlayer, type: String, persistent: Boolean): String? {
        val entityType = resolve(type)
            ?: return errPlaceholdersOr("invalidMob", "%prefix% &c'%type%' is not a valid mob.", target)
                .put("type", type)
                .toString()
        setField(target, "type", entityType)
        if (persistent) addFlag(target, "persistent")
        return null
    }

    fun peekaboo(player: RbPlayer, location: Location) {
        if (!hasPlayer(player))
            return

        runTask {
            val type = getField<EntityType>(player, "type")
            if (type != null) {
                val bukkitType = org.bukkit.entity.EntityType.valueOf(type.name.key.uppercase())
                val entity = location.world?.spawnEntity(location, bukkitType)
                (entity as? Creature)?.target = player.bukkit
            }

            if (!hasFlag(player, "persistent"))
                removePlayer(player)
        }
    }

    fun peekaboo(player: Player, location: Location) = peekaboo(player.rb, location)
}