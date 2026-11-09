package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.*
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*

@CommandInfo(
    name = "bomb",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.DISPENSER,
    description = "Fires projectiles from thin air at a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.STRING,
            completions = ["arrow", "snowball", "egg", "fireball"],
            strictCompletion = true,
            purpose = "Projectile",
            description = "The projectile to fire",
            default = "arrow"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Bomb : BaseToggleTroll() {
    val types: List<Class<out Projectile>> = listOf(
        Arrow::class.java,
        Snowball::class.java,
        Egg::class.java,
        Fireball::class.java,
    )
    val carpetBombOperations: MutableMap<UUID, Class<out Projectile>> = mutableMapOf()

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val type: Class<out Projectile> = if (args.size > 2) {
            types.firstOrNull { it.simpleName.equals(args[2], ignoreCase = true) } ?: types.first()
        } else {
            Arrow::class.java
        }

        carpetBombOperations[target.uuid] = type

        addTaskTimer(target, {
            val yaw = MathUtil.random(0.0, 360.0)
            val location = target.location.clone().add(0.0, target.eyeHeight / 2.0, 0.0)
            val projectile = target.world.spawn(location.clone().add(
                MathUtil.rcos(yaw) * 5.0,
                2.0,
                MathUtil.rsin(yaw) * 5.0
            ), type)
            val velocity = location.toVector().subtract(projectile.location.toVector()).normalize().multiply(1.25)
            projectile.velocity = velocity
        }, 0, 1, true)

        return when(type) {
            Arrow::class.java -> placeholdersOr("initArrow", "%prefix% %c1%%player% %c2%is now a pincushion").put("player", target.name).toString()
            Snowball::class.java -> placeholdersOr("initSnowball", "%prefix% %c1%%player% %c2%is now in a snow storm").put("player", target.name).toString()
            Egg::class.java -> placeholdersOr("initEgg", "%prefix% %c1%%player% %c2%is now HowToBasic").put("player", target.name).toString()
            //cover fireball & whatever else
            else -> placeholdersOr("init", "%prefix% %c1%%player% %c2%is now being carpet bombed").put("player", target.name).toString()
        }
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        val fallback = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer being carpet bombed").put("player", target.name).toString()
        val type = carpetBombOperations.remove(target.uuid) ?: return fallback
        return when(type) {
            Arrow::class.java -> placeholdersOr("deinitArrow", "%prefix% %c1%%player% %c2%is no longer a pincushion").put("player", target.name).toString()
            Snowball::class.java -> placeholdersOr("deinitSnowball", "%prefix% %c1%%player% %c2%is no longer in a snow storm").put("player", target.name).toString()
            Egg::class.java -> placeholdersOr("deinitEgg", "%prefix% %c1%%player% %c2%is in fact not HowToBasic").put("player", target.name).toString()
            else -> fallback
        }
    }

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        carpetBombOperations.remove(player.uuid)
    }
}