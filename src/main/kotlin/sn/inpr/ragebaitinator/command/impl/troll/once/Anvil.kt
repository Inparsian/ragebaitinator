package sn.inpr.ragebaitinator.command.impl.troll.once

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.FallingBlock
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.material.MaterialData
import sn.inpr.ragebaitinator.Main.Companion.runTaskLater
import sn.inpr.ragebaitinator.command.def.Args
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.Conditions
import sn.inpr.ragebaitinator.listeners.EntityChangeBlockSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.PlayerUtil

@CommandInfo(
    name = "anvil",
    category = CommandInfo.Category.HAZARDS,
    icon = Material.ANVIL,
    description = "X marks the spot.",
)
class Anvil : BaseTroll(), PlayerMoveSubscriber, EntityChangeBlockSubscriber {
    private val anvils = mutableMapOf<FallingBlock, RbPlayer>()

    fun pathObstructed(target: RbPlayer): Boolean {
        if (!Bukkit.isPrimaryThread() && !target.location.chunk.isLoaded)
            return true
        val anvilLocation = target.location.clone().add(0.0, 12.0, 0.0)
        val targetY = anvilLocation.y - 12
        for (i in anvilLocation.y.toInt() downTo targetY.toInt()) {
            if (anvilLocation.block.type.isSolid())
                return true
            anvilLocation.y = i.toDouble()
        }
        return false
    }

    override fun getConditions(target: RbPlayer) = Conditions()
        .notDead(target)
        .notInVoid(target)
        .put("An anvil must not already be falling on this player.", "anvilExists", anvils.containsValue(target))
        .put("The anvil's path must not be obstructed.", "pathObstructed", pathObstructed(target))

    fun thwomp(anvil: FallingBlock) {
        val target = anvils.remove(anvil) ?: return
        anvil.location.world?.playSound(anvil.location, Sound.BLOCK_ANVIL_LAND, 1f, 1f)
        target.damage(PlayerUtil.MAX_POSSIBLE_DAMAGE, anvil)
        anvil.remove()
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        val anvilLocation = LocationUtil.blockify(target.location.clone().add(0.0, 12.0, 0.0), true)
        @Suppress("DEPRECATION")
        val anvil: FallingBlock = target.world.getBlockAt(anvilLocation).world.spawnFallingBlock(anvilLocation, MaterialData(Material.ANVIL))
        target.teleport(target.location)
        anvils[anvil] = target
        runTaskLater({ thwomp(anvil) }, 25)
        return placeholdersOr("init", "%prefix% %c2%Dropped an anvil on %c1%%player%")
            .put("player", target.name)
            .toString()
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        event.cancelIf(anvils.containsValue(event.player.rb) && event.from.distance(event.to ?: event.from) > 0)
    }

    override fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        thwomp(event.entity as? FallingBlock ?: return)
    }

    override fun onDisable() {
        anvils.keys.forEach { it.remove() }
        anvils.clear()
    }
}