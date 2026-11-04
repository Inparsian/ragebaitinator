package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.command.CommandSender
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.EntityExplodeSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "creep",
    description = "Spawns a few charged creepers at the player's location",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "Harmless",
            description = "Whether the creepers shouldn't damage the world",
            default = "true"
        )
    ]
)
class Creep : BaseTroll(), EntityDamageSubscriber, EntityExplodeSubscriber {
    val markKey = Main.makeKey("ragebaitinatorCreepTroll")

    fun mark(entity: Entity, harmless: Boolean)
        = entity.persistentDataContainer.set(markKey, PersistentDataType.BOOLEAN, harmless)
    fun getMark(entity: Entity): Boolean?
        = entity.persistentDataContainer.get(markKey, PersistentDataType.BOOLEAN)

    override fun getConditions(target: Player)
        = Conditions().notDead(target).notInVoid(target)

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        for (i in 1..4) {
            val creeper = target.world.spawn(target.location, Creeper::class.java)
            mark(creeper, args.getOrNull(2)?.toBoolean() ?: true)

            val velocity = Vector(MathUtil.rcos(i * 90), 0.2, MathUtil.rsin(i * 90)).multiply(0.5)
            creeper.velocity = velocity
            creeper.isPowered = true
            creeper.target = target
            creeper.maxFuseTicks = 20
            creeper.ignite()
        }
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now playing with explosives")
            .put("player", target.name)
            .toString()
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        // make em invincible
        if (getMark(event.entity) != null)
            event.isCancelled = true
    }

    override fun onEntityExplode(event: EntityExplodeEvent) {
        if (getMark(event.entity) == true) {
            event.isCancelled = true
            LocationUtil.createSafeExplosion(event.entity.location, 2f)
            event.entity.remove()
        }
    }
}