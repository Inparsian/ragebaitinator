package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Creeper
import org.bukkit.entity.Entity
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.EntityExplodeSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "creep",
    category = CommandInfo.Category.EXPLOSIVES,
    icon = Material.CREEPER_HEAD,
    description = "Spawns a few charged creepers at the player's location",
)
class Creep : BaseTroll(), EntityDamageSubscriber, EntityExplodeSubscriber {
    private val harmless = param<Boolean>("Harmless", "Whether the creepers shouldn't damage the world", default = true)

    override val syntax = withTarget(harmless)

    val markKey = Main.makeKey("ragebaitinatorCreepTroll")

    fun mark(entity: Entity, harmless: Boolean)
        = entity.persistentDataContainer.set(markKey, PersistentDataType.BOOLEAN, harmless)
    fun getMark(entity: Entity): Boolean?
        = entity.persistentDataContainer.get(markKey, PersistentDataType.BOOLEAN)

    override fun getConditions(target: RbPlayer)
        = Conditions().notDead(target).notInVoid(target)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        for (i in 1..4) {
            val creeper = target.world.spawn(target.location, Creeper::class.java)
            mark(creeper, args[harmless])

            val velocity = Vector(MathUtil.rcos(i * 90), 0.2, MathUtil.rsin(i * 90)).multiply(0.5)
            creeper.velocity = velocity
            creeper.isPowered = true
            creeper.target = target.bukkit
            creeper.maxFuseTicks = 20
            creeper.ignite()
        }
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now playing with explosives")
            .put("player", target.name)
            .toString()
    }

    // make em invincible
    override fun onEntityDamage(event: EntityDamageEvent) = event.cancelIf(getMark(event.entity) != null)

    override fun onEntityExplode(event: EntityExplodeEvent) {
        event.cancelIfAndThen(getMark(event.entity) == true) {
            LocationUtil.createSafeExplosion(event.entity.location, 2f)
            event.entity.remove()
        }
    }
}