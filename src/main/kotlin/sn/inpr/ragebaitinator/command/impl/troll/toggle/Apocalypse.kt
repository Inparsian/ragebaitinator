package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@CommandInfo(
    name = "apocalypse",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.WITHER_SKELETON_SKULL,
    description = "The dead walk. Monsters spawn around a player much faster than usual.",
    attributes = [
        CommandInfo.Attribute.LOGOUT, CommandInfo.Attribute.DEATH
    ]
)
class Apocalypse : BaseToggleTroll() {
    private val rate = param<Double>("Rate", "How many monsters spawn around them every second (min 0.1, max 20).", default = 1.0, range = 0.1..20.0)
    private val cap = param<Int>("Cap", "The most monsters that can be alive at once from this (min 1, max 200).", default = 40, range = 1..200)

    override val syntax = withTarget(rate, cap)

    private companion object {
        const val MIN_DISTANCE = 24.0
        const val MAX_DISTANCE = 128.0
        const val MAX_BLOCK_LIGHT = 0
        const val MAX_SKY_LIGHT = 7
        const val HEIGHT_RANGE = 24
        const val SPAWN_HEIGHT = 3 // the tallest monster in the pool is an enderman
        const val ATTEMPTS = 24
        const val PERIOD = 10L
        const val MAX_SPAWNS_PER_PERIOD = 5

        // some of them are in more than once, which is how they are made more likely
        val OVERWORLD = listOf(
            EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SKELETON,
            EntityType.SPIDER, EntityType.SPIDER, EntityType.CREEPER, EntityType.CREEPER, EntityType.WITCH,
            EntityType.ENDERMAN
        )
        val NETHER = listOf(
            EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.MAGMA_CUBE, EntityType.SKELETON, EntityType.WITHER_SKELETON
        )
        val END = listOf(EntityType.ENDERMAN)

        fun harmful(block: Block): Boolean = block.isLiquid || block.type.name.contains("FIRE") ||
                block.type == Material.COBWEB || block.type == Material.SWEET_BERRY_BUSH
        fun harmfulGround(block: Block): Boolean = block.type == Material.MAGMA_BLOCK || block.type == Material.CACTUS ||
                block.type == Material.CAMPFIRE || block.type == Material.SOUL_CAMPFIRE
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        setField(target, "rate", args[rate])
        setField(target, "cap", args[cap])
        setField(target, "budget", 0.0)
        setField(target, "mobs", ConcurrentHashMap.newKeySet<UUID>())
        setTaskTimer(target, { tick(target) }, 20, PERIOD)
        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now living through the apocalypse", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is safe from the apocalypse for now", target)
            .toString()

    private fun tick(player: RbPlayer) {
        val body = player.bukkit ?: return
        val world = body.world
        if (player.isDead || world.difficulty == Difficulty.PEACEFUL)
            return

        // the monsters that have died, or despawned, or are in a chunk that isn't loaded, don't count anymore
        val mobs = getField<MutableSet<UUID>>(player, "mobs") ?: return
        mobs.removeIf { Bukkit.getEntity(it)?.isValid != true }

        val rate = getField<Double>(player, "rate") ?: 1.0
        val cap = getField<Int>(player, "cap") ?: 40
        var budget = ((getField<Double>(player, "budget") ?: 0.0) + rate * PERIOD / 20.0).coerceAtMost(MAX_SPAWNS_PER_PERIOD.toDouble())
        var spawned = 0
        while (budget >= 1.0 && spawned < MAX_SPAWNS_PER_PERIOD && mobs.size < cap) {
            budget -= 1.0
            spawned++
            mobs += spawn(world, body.location) ?: continue
        }
        setField(player, "budget", if (mobs.size >= cap) 0.0 else budget)
    }

    private fun spawn(world: World, origin: Location): UUID? {
        val pool = when (world.environment) {
            World.Environment.NETHER -> NETHER
            World.Environment.THE_END -> END
            else -> OVERWORLD
        }

        repeat(ATTEMPTS) {
            val spot = candidate(world, origin) ?: return@repeat
            return world.spawnEntity(spot, pool.random()).uniqueId
        }
        return null
    }

    private fun candidate(world: World, origin: Location): Location? {
        val angle = MathUtil.random(0.0, Math.PI * 2)
        val distance = MathUtil.random(MIN_DISTANCE, MAX_DISTANCE)
        val x = floor(origin.x + cos(angle) * distance).toInt()
        val z = floor(origin.z + sin(angle) * distance).toInt()

        // a chunk that is loaded but isn't ticking entities would leave a monster frozen in place
        if (!world.isChunkLoaded(x shr 4, z shr 4) || world.getChunkAt(x shr 4, z shr 4).loadLevel != Chunk.LoadLevel.ENTITY_TICKING)
            return null

        val low = world.minHeight + 2
        val high = world.maxHeight - SPAWN_HEIGHT - 1
        if (low >= high)
            return null

        var y = if (MathUtil.randomBool())
            world.getHighestBlockYAt(x, z) + 1
        else
            MathUtil.random(origin.blockY - HEIGHT_RANGE, origin.blockY + HEIGHT_RANGE)
        y = y.coerceIn(low, high)

        var steps = 0
        while (steps++ < 12 && y > low && world.getBlockAt(x, y - 1, z).isPassable)
            y--

        val feet = world.getBlockAt(x, y, z)
        val ground = world.getBlockAt(x, y - 1, z)
        if (ground.isPassable || !ground.type.isSolid || harmfulGround(ground))
            return null
        if ((0 until SPAWN_HEIGHT).any { world.getBlockAt(x, y + it, z).let { block -> !block.isPassable || harmful(block) } })
            return null
        if (feet.lightFromBlocks > MAX_BLOCK_LIGHT || feet.lightFromSky > MAX_SKY_LIGHT)
            return null

        val spot = Location(world, x + 0.5, y.toDouble(), z + 0.5)
        if (world.players.any { it.location.distanceSquared(spot) < MIN_DISTANCE * MIN_DISTANCE })
            return null
        return spot
    }
}
