package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Cow
import org.bukkit.entity.Creature
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import java.lang.reflect.Method

private const val PURSUIT_SPEED_MULTIPLIER = 2.4

@CommandInfo(
    name = "stampede",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.COW_SPAWN_EGG,
    description = "Unleashes a herd of cows on a player.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
        CommandSyntax(
            type = CommandSyntax.Type.BOOLEAN,
            purpose = "From Shooter",
            description = "Should the cows be spawned from the shooter?",
            default = "false"
        )
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Stampede : BaseToggleTroll(), EntityDamageSubscriber {
    class CowNavigation(cow: Cow) {
        private val navigation: Any
        private val moveTo: Method
        private val stop: Method

        init {
            val handle = cow.javaClass.getMethod("getHandle").invoke(cow)
            navigation = handle.javaClass.getMethod("getNavigation").invoke(handle)
            moveTo = navigation.javaClass.getMethod(
                "moveTo", Double::class.javaPrimitiveType, Double::class.javaPrimitiveType,
                Double::class.javaPrimitiveType, Double::class.javaPrimitiveType
            )
            require(moveTo.returnType == Boolean::class.javaPrimitiveType)
            stop = navigation.javaClass.getMethod("stop")
        }

        fun moveTo(location: Location) {
            moveTo.invoke(navigation, location.x, location.y, location.z, PURSUIT_SPEED_MULTIPLIER)
        }

        fun stop() {
            stop.invoke(navigation)
        }
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String {
        val fromShooter = args.getOrNull(2)?.toBoolean() ?: false
        val herdLocation = if (sender is Player && fromShooter && sender.world == target.world)
            sender.location.clone()
        else
            target.location.clone()

        val positions = safePositions(herdLocation, target.location)
        if (positions.size < 6) {
            removePlayer(target, "No safe herd spawn")
            return "&cCould not find enough safe ground for the herd."
        }

        val herd = mutableSetOf<Creature>()
        val navigators = mutableMapOf<Creature, CowNavigation>()
        setField(target, "herd", herd)
        try {
            for (position in positions) {
                val cow = position.world!!.spawn(position, Cow::class.java)
                herd.add(cow)
                navigators[cow] = CowNavigation(cow)
            }
        } catch (_: Exception) {
            removePlayer(target, "Herd spawning failed")
            return "&cCould not spawn the herd or initialize cow navigation on this server."
        }

        var ticks = 0
        addTaskTimer(target, {
            val herd = getField<MutableSet<Creature>>(target, "herd") ?: return@addTaskTimer
            val refreshRoute = ticks++ % 10 == 0
            try {
                for (cow in herd.toList()) {
                    if (!cow.isValid || cow.isDead) {
                        herd.remove(cow)
                        navigators.remove(cow)
                        cow.remove()
                        continue
                    }
                    if (cow.world != target.world) {
                        if (refreshRoute) navigators[cow]?.stop()
                        continue
                    }

                    // damage
                    val bukkitTarget = target.bukkit
                    if (bukkitTarget != null && cow.location.distanceSquared(target.location) <= 1.0) {
                        val damageSource = DamageSource.builder(DamageType.MOB_ATTACK)
                            .withCausingEntity(cow)
                            .withDirectEntity(cow)
                            .withDamageLocation(cow.location)
                            .build()
                        val event = EntityDamageByEntityEvent(
                            cow,
                            bukkitTarget,
                            EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                            damageSource,
                            1.0
                        )
                        Bukkit.getPluginManager().callEvent(event)
                        if (!event.isCancelled)
                            target.damage(event.damage, cow)
                    }

                    if (refreshRoute) navigators[cow]?.moveTo(target.location)
                }
                if (herd.isEmpty()) removePlayer(target, "Herd is gone")
            } catch (exception: Exception) {
                Bukkit.getLogger().warning("Stampede navigation failed for ${target.name}: $exception")
                removePlayer(target, "Herd navigation failed")
                sender.sendMessage("Stampede stopped because cow navigation failed.")
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%just made some cows really angry")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer being chased by cows")
            .put("player", target.name)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) {
        getField<MutableSet<Creature>>(player, "herd")?.forEach { it.remove() }
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        val creature = event.entity as? Creature ?: return
        event.cancelIf(fieldValues<MutableSet<Creature>>("herd").any { creature in it })
    }

    private fun safePositions(origin: Location, target: Location): List<Location> {
        val world = origin.world ?: return emptyList()
        val positions = mutableListOf<Location>()
        for (radius in 0..5) {
            for (x in -radius..radius) for (z in -radius..radius) {
                if (maxOf(kotlin.math.abs(x), kotlin.math.abs(z)) != radius) continue
                val centerX = origin.blockX + x + 0.5
                val centerZ = origin.blockZ + z + 0.5
                if (!farEnough(centerX, centerZ, origin.x, origin.z) ||
                    !farEnough(centerX, centerZ, target.x, target.z)) continue
                for (y in listOf(0, 1, -1, 2, -2)) {
                    val bx = origin.blockX + x
                    val by = origin.blockY + y
                    val bz = origin.blockZ + z
                    if (by <= world.minHeight || by + 1 >= world.maxHeight) continue
                    val support = world.getBlockAt(bx, by - 1, bz)
                    if (!support.type.isOccluding || support.type == Material.MAGMA_BLOCK) continue
                    if (!world.getBlockAt(bx, by, bz).isEmpty || !world.getBlockAt(bx, by + 1, bz).isEmpty) continue
                    positions.add(Location(world, bx + 0.5, by.toDouble(), bz + 0.5))
                    if (positions.size == 6) return positions
                    break
                }
            }
        }
        return positions
    }

    internal fun farEnough(x: Double, z: Double, referenceX: Double, referenceZ: Double): Boolean {
        val dx = x - referenceX
        val dz = z - referenceZ
        return dx * dx + dz * dz >= 9.0
    }
}