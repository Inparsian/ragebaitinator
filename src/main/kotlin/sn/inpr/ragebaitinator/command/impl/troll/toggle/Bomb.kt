package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.command.CommandSender
import org.bukkit.entity.Arrow
import org.bukkit.entity.Egg
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.Snowball
import org.bukkit.event.player.PlayerItemConsumeEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.UUID
import kotlin.jvm.java

@CommandInfo(
    name = "bomb",
    description = "Fires projectiles from thin air at a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
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

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val type: Class<out Projectile> = if (args.size > 2) {
            types.firstOrNull { it.simpleName.equals(args[2], ignoreCase = true) } ?: types.first()
        } else {
            Arrow::class.java
        }

        carpetBombOperations[target.uniqueId] = type

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
            Arrow::class.java -> "&c${target.name} &fis now a pincushion"
            Snowball::class.java -> "&c${target.name} &fis now in a snow storm"
            Egg::class.java -> "&c${target.name} &fis now HowToBasic"
            else -> "&c${target.name} &fis now being carpet bombed" //cover fireball & whatever else
        }
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        val type = carpetBombOperations.remove(target.uniqueId) ?: return "&c${target.name} &fis no longer being carpet bombed"
        return when(type) {
            Arrow::class.java -> "&c${target.name} &fis no longer a pincushion"
            Snowball::class.java -> "&c${target.name} &fis no longer in a snow storm"
            Egg::class.java -> "&c${target.name} &fis in fact not HowToBasic"
            else -> "&c${target.name} &fis no longer being carpet bombed"
        }
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        carpetBombOperations.remove(player.uniqueId)
    }
}