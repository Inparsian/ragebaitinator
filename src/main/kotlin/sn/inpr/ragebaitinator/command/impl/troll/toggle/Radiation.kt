package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Animals
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.database.Blocks
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.MathUtil
import sn.inpr.ragebaitinator.utility.Reflection

@CommandInfo(
    name = "radiation",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.GLOWSTONE_DUST,
    description = "Makes a player radioactive, slowly killing all players, animals, and plants around them.",
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class Radiation : BaseToggleTroll() {
    private val playersParam = param<Boolean>("Players", "Whether or not to hurt players.", default = true)
    private val animals = param<Boolean>("Animals", "Whether or not to hurt animals.", default = true)
    private val plants = param<Boolean>("Plants", "Whether or not to hurt plants.", default = true)

    override val syntax = withTarget(playersParam, animals, plants)

    companion object {
        val flowers = if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_13))
            setOf(
                Material.POPPY,
                Material.DANDELION,
                Material.BLUE_ORCHID,
                Material.ALLIUM,
                Material.AZURE_BLUET,
                Material.RED_TULIP,
                Material.ORANGE_TULIP,
                Material.WHITE_TULIP,
                Material.PINK_TULIP,
                Material.OXEYE_DAISY,
                Material.SUNFLOWER,
                Material.LILAC,
                Material.ROSE_BUSH,
                Material.PEONY,
                Material.TALL_GRASS,
                Material.LARGE_FERN
            )
        else
            setOf(
                Reflection.getMaterial("RED_ROSE"),
                Reflection.getMaterial("YELLOW_FLOWER"),
                Reflection.getMaterial("DOUBLE_PLANT"),
                Reflection.getMaterial("LONG_GRASS")
            )

        val leaves = Material.entries.filter { it.name.endsWith("LEAVES") }
    }

    private fun rollbackOwner(player: RbPlayer) = "radiation:${player.uuid}"

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        if (args[playersParam]) addFlag(target, "players")
        if (args[animals]) addFlag(target, "animals")
        if (args[plants]) addFlag(target, "plants")

        addTaskTimer(target, {
            if (hasFlag(target, "plants")) {
                val sphere = LocationUtil.sphere(target.location.toVector(), 5.0)
                val changes = mutableListOf<Pair<Block, Material>>()
                for (vector in sphere) {
                    if (!MathUtil.randomBool(0.0075))
                        continue

                    val block = target.world.getBlockAt(vector.toLocation(target.world))
                    if (block.type == Material.GRASS_BLOCK || block.type.name == "GRASS") {
                        changes.add(Pair(block, Material.DIRT))
                    } else if (leaves.contains(block.type) || flowers.contains(block.type)) {
                        changes.add(Pair(block, Material.AIR))
                    }
                }
                Blocks.place(rollbackOwner(target), changes)
            }

            if (hasFlag(target, "players") || hasFlag(target, "animals")) {
                for (entity in target.nearbyEntities(5.0)) {
                    if (entity !is LivingEntity)
                        continue

                    val damage = MathUtil.random(1.0, 2.0)
                    if (entity is Player && hasFlag(target, "players")) {
                        entity.damage(damage)
                    } else if (entity is Animals && hasFlag(target, "animals")) {
                        entity.damage(damage)
                    }
                }
            }
        }, 0, 1, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now emitting a gajillion sieverts of radiation", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String =
        placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer immensely radioactive", target)
            .toString()

    override fun onRemovePlayer(player: RbPlayer, reason: String) = Blocks.release(rollbackOwner(player))
}