package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.event.entity.ProjectileHitEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.ProjectileHitSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil

@CommandInfo(
    name = "bludger",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.FIRE_CHARGE,
    description = "Sends a homing fireball after a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "From Shooter",
            description = "Should the Bludger come from the shooter?",
            default = "true"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Bludger : BaseToggleTroll(), ProjectileHitSubscriber {
    private val bludgers: MutableMap<RbPlayer, Projectile> = mutableMapOf()

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val fromShooter = args.getOrNull(2)?.toBoolean() ?: true
        val bludgerLocation = if (sender is Player && fromShooter)
            sender.eyeLocation.clone().add(sender.eyeLocation.direction.multiply(2))
        else {
            LocationUtil
                .locationRelativeYaw(target, 180, 6.0)
                .add(0.0, 2.0, 0.0)
        }

        val bludger = bludgerLocation.world!!.spawn(bludgerLocation, Snowball::class.java)
        val velocity = target.location.clone().subtract(bludger.location).toVector()
        if (LocationUtil.isFinite(velocity))
            bludger.velocity = velocity
        bludger.fireTicks = 1_000_000
        bludger.setGravity(false)
        bludgers[target] = bludger

        addTaskTimer(target, {
            if (bludgers.containsKey(target)) {
                // the bludger shalt not have a foot fetish
                val plusOne = target.location.clone().add(0.0, 1.0, 0.0)
                val targetBludger = bludgers[target]!!
                val targetVelocity = plusOne
                    .subtract(targetBludger.location)
                    .toVector()
                    .normalize()
                    .multiply(0.25)
                if (LocationUtil.isFinite(targetVelocity))
                    targetBludger.velocity = targetVelocity
                bludger.fireTicks = 1_000_000
                // occasionally they do this weird wiggle thing inside a player's hitbox
                // and never actually hit???
                if (bludger.location.distance(plusOne) <= 0.3)
                    kaboom(target, bludger)
            } else {
                removeTask(target)
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being chased by a flying fire ball thing")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer being chased by a flying fire ball thing")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        bludgers.remove(player)?.remove()
    }

    fun kaboom(target: RbPlayer, projectile: Projectile) {
        LocationUtil.createSafeExplosion(projectile.location, 4.0f)
        removePlayer(target)
    }

    override fun onProjectileHit(event: ProjectileHitEvent) {
        if (bludgers.containsValue(event.entity)) {
            val target = bludgers.entries.first { it.value == event.entity }.key
            kaboom(target, event.entity)
        }
    }
}