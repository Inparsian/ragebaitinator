package sn.inpr.ragebaitinator.command.impl.troll.toggle

import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.AreaEffectCloudApplyEvent
import org.bukkit.event.entity.PotionSplashEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.AreaEffectCloudApplySubscriber
import sn.inpr.ragebaitinator.listeners.PlayerItemConsumeSubscriber
import sn.inpr.ragebaitinator.listeners.PotionSplashSubscriber
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.utility.MathUtil

@CommandInfo(
    name = "randompotion",
    category = CommandInfo.Category.INVENTORY,
    icon = Material.POTION,
    description = "Mystery flavor. Potions a player drinks or is hit with give them a random effect instead.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.LOGOUT
    ]
)
class RandomPotion : BaseToggleTroll(), PlayerItemConsumeSubscriber, PotionSplashSubscriber, AreaEffectCloudApplySubscriber {
    private companion object {
        // these would start a raid or a trial, and that's a different troll
        val excluded = setOf(PotionEffectType.BAD_OMEN, PotionEffectType.RAID_OMEN, PotionEffectType.TRIAL_OMEN)
    }

    private val pool: List<PotionEffectType> by lazy { Registry.EFFECT.filterNot { it in excluded } }

    override fun init(sender: CommandSender, target: RbPlayer, args: Array<out String>): String
        = placeholdersOr("init", "%prefix% %c1%%player%%c2%'s potions are now a lottery")
            .put("player", target.name)
            .toString()

    override fun deinit(sender: CommandSender, target: RbPlayer): String
        = placeholdersOr("deinit", "%prefix% %c1%%player%%c2%'s potions do what they say on the bottle again")
            .put("player", target.name)
            .toString()

    private fun randomEffect(): PotionEffect {
        val type = pool.random()
        return if (type.isInstant)
            PotionEffect(type, 1, 0)
        else
            PotionEffect(type, MathUtil.random(200, 601), MathUtil.random(0, 2))
    }

    // what they drink is swallowed, and what it does is what they get instead. water bottles count, they're potions too
    override fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        if (event.isCancelled || !hasPlayer(player) || event.item.type != Material.POTION)
            return

        event.isCancelled = true
        player.inventory.setItem(event.hand, ItemStack(Material.GLASS_BOTTLE))
        player.addPotionEffect(randomEffect())
    }

    // the potion hits them with nothing at all, which makes room for the one they actually get
    override fun onPotionSplash(event: PotionSplashEvent) {
        for (entity in event.affectedEntities.toList()) {
            val player = entity as? Player ?: continue
            if (!hasPlayer(player))
                continue

            event.setIntensity(player, 0.0)
            player.addPotionEffect(randomEffect())
        }
    }

    // a cloud applies itself over and over, so they get a new one every time it does
    override fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) {
        val hit = event.affectedEntities.filterIsInstance<Player>().filter { hasPlayer(it) }
        event.affectedEntities.removeAll(hit.toSet())
        hit.forEach { it.addPotionEffect(randomEffect()) }
    }
}
