package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.*
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "bomb",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.DISPENSER,
    description = "Fires projectiles from thin air at a player.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Bomb : BaseToggleTroll() {
    @Suppress("unused")
    enum class Ammo(
        val type: Class<out Projectile>,
        val initKey: String, val initText: String,
        val deinitKey: String, val deinitText: String,
    ) {
        ARROW(
            Arrow::class.java,
            "initArrow", "%prefix% %c1%%player% %c2%is now a pincushion",
            "deinitArrow", "%prefix% %c1%%player% %c2%is no longer a pincushion"
        ),
        SNOWBALL(
            Snowball::class.java,
            "initSnowball", "%prefix% %c1%%player% %c2%is now in a snow storm",
            "deinitSnowball", "%prefix% %c1%%player% %c2%is no longer in a snow storm"
        ),
        EGG(
            Egg::class.java,
            "initEgg", "%prefix% %c1%%player% %c2%is now HowToBasic",
            "deinitEgg", "%prefix% %c1%%player% %c2%is in fact not HowToBasic"
        ),
        // the generic messages cover this and whatever else gets added
        FIREBALL(
            Fireball::class.java,
            "init", "%prefix% %c1%%player% %c2%is now being carpet bombed",
            "deinit", "%prefix% %c1%%player% %c2%is no longer being carpet bombed"
        ),
    }

    private val projectile = param<Ammo>("Projectile", "The projectile to fire", default = Ammo.ARROW)

    override val syntax = withTarget(projectile)

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val ammo = args[projectile]
        setField(target, "ammo", ammo)

        addTaskTimer(target, {
            val yaw = MathUtil.random(0.0, 360.0)
            val location = target.location.clone().add(0.0, target.eyeHeight / 2.0, 0.0)
            val projectile = target.world.spawn(location.clone().add(
                MathUtil.rcos(yaw) * 5.0,
                2.0,
                MathUtil.rsin(yaw) * 5.0
            ), ammo.type)
            val velocity = location.toVector().subtract(projectile.location.toVector()).normalize().multiply(1.25)
            projectile.velocity = velocity
        }, 0, 1, true)

        return placeholdersOr(ammo.initKey, ammo.initText).put("player", target.name).toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        val ammo = takeField<Ammo>(target, "ammo") ?: Ammo.FIREBALL
        return placeholdersOr(ammo.deinitKey, ammo.deinitText).put("player", target.name).toString()
    }
}