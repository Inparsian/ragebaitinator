package sn.inpr.ragebaitinator.listeners

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerVelocityEvent
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo

interface PlayerJoinSubscriber { fun onPlayerJoin(event: PlayerJoinEvent) }
interface PlayerQuitSubscriber { fun onPlayerQuit(event: PlayerQuitEvent) }
interface AsyncPlayerChatSubscriber { fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) }
interface PlayerTeleportSubscriber { fun onPlayerTeleport(event: PlayerTeleportEvent) }
interface PlayerMoveSubscriber { fun onPlayerMove(event: PlayerMoveEvent) }
interface PlayerVelocitySubscriber { fun onPlayerVelocity(event: PlayerVelocityEvent) }
interface PlayerInteractSubscriber { fun onPlayerInteract(event: PlayerInteractEvent) }
interface BlockBreakSubscriber { fun onBlockBreak(event: BlockBreakEvent) }
interface BlockPlaceSubscriber { fun onBlockPlace(event: BlockPlaceEvent) }
interface EntityDamageByEntitySubscriber { fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) }
interface EntityDamageSubscriber { fun onEntityDamage(event: EntityDamageEvent) }
interface PlayerDeathSubscriber { fun onPlayerDeath(event: PlayerDeathEvent) }
interface EntityDeathSubscriber { fun onEntityDeath(event: EntityDeathEvent) }
interface PlayerRespawnSubscriber { fun onPlayerRespawn(event: PlayerRespawnEvent) }
interface EntityShootBowSubscriber { fun onEntityShootBow(event: EntityShootBowEvent) }
interface EntityPickupItemSubscriber { fun onEntityPickupItem(event: EntityPickupItemEvent) }
interface ProjectileLaunchSubscriber { fun onProjectileLaunch(event: ProjectileLaunchEvent) }
interface ProjectileHitSubscriber { fun onProjectileHit(event: ProjectileHitEvent) }
interface EntityExplodeSubscriber { fun onEntityExplode(event: EntityExplodeEvent) }
interface PlayerItemConsumeSubscriber { fun onPlayerItemConsume(event: PlayerItemConsumeEvent) }
interface EntityToggleGlideSubscriber { fun onEntityToggleGlide(event: EntityToggleGlideEvent) }
interface PlayerBedEnterSubscriber { fun onPlayerBedEnter(event: PlayerBedEnterEvent) }
interface PlayerFishSubscriber { fun onPlayerFish(event: PlayerFishEvent) }

object BukkitEventListener : Listener {
    fun removeToggleTrollsWithAttribute(player: Player, attribute: CommandInfo.Attribute, reason: String) {
        CommandManager.commands
            .filterIsInstance<BaseToggleTroll>()
            .filter { it.info.attributes.contains(attribute) }
            .forEach { it.removePlayer(player, reason) }
    }

    fun interceptBukkitEvent(event: Event) {
        for (command in CommandManager.commands) {
            if (command is PlayerJoinSubscriber && event is PlayerJoinEvent) command.onPlayerJoin(event)
            if (command is PlayerQuitSubscriber && event is PlayerQuitEvent) command.onPlayerQuit(event)
            if (command is AsyncPlayerChatSubscriber && event is AsyncPlayerChatEvent) command.onAsyncPlayerChat(event)
            if (command is PlayerTeleportSubscriber && event is PlayerTeleportEvent) command.onPlayerTeleport(event)
            if (command is PlayerMoveSubscriber && event is PlayerMoveEvent) command.onPlayerMove(event)
            if (command is PlayerVelocitySubscriber && event is PlayerVelocityEvent) command.onPlayerVelocity(event)
            if (command is PlayerInteractSubscriber && event is PlayerInteractEvent) command.onPlayerInteract(event)
            if (command is BlockBreakSubscriber && event is BlockBreakEvent) command.onBlockBreak(event)
            if (command is BlockPlaceSubscriber && event is BlockPlaceEvent) command.onBlockPlace(event)
            if (command is EntityDamageByEntitySubscriber && event is EntityDamageByEntityEvent) command.onEntityDamageByEntity(event)
            if (command is EntityDamageSubscriber && event is EntityDamageEvent) command.onEntityDamage(event)
            if (command is PlayerDeathSubscriber && event is PlayerDeathEvent) command.onPlayerDeath(event)
            if (command is EntityDeathSubscriber && event is EntityDeathEvent) command.onEntityDeath(event)
            if (command is PlayerRespawnSubscriber && event is PlayerRespawnEvent) command.onPlayerRespawn(event)
            if (command is EntityShootBowSubscriber && event is EntityShootBowEvent) command.onEntityShootBow(event)
            if (command is EntityPickupItemSubscriber && event is EntityPickupItemEvent) command.onEntityPickupItem(event)
            if (command is ProjectileLaunchSubscriber && event is ProjectileLaunchEvent) command.onProjectileLaunch(event)
            if (command is ProjectileHitSubscriber && event is ProjectileHitEvent) command.onProjectileHit(event)
            if (command is EntityExplodeSubscriber && event is EntityExplodeEvent) command.onEntityExplode(event)
            if (command is PlayerItemConsumeSubscriber && event is PlayerItemConsumeEvent) command.onPlayerItemConsume(event)
            if (command is EntityToggleGlideSubscriber && event is EntityToggleGlideEvent) command.onEntityToggleGlide(event)
            if (command is PlayerBedEnterSubscriber && event is PlayerBedEnterEvent) command.onPlayerBedEnter(event)
            if (command is PlayerFishSubscriber && event is PlayerFishEvent) command.onPlayerFish(event)
        }

        // remove corresponding attributes
        when (event) {
            is PlayerQuitEvent -> removeToggleTrollsWithAttribute(event.player, CommandInfo.Attribute.LOGOUT, "logout")
            is EntityDeathEvent -> removeToggleTrollsWithAttribute(event.entity as? Player ?: return, CommandInfo.Attribute.DEATH, "death")
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerTeleport(event: PlayerTeleportEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerMove(event: PlayerMoveEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerVelocity(event: PlayerVelocityEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeath(event: EntityDeathEvent) = interceptBukkitEvent(event) // EntityDeath & PlayerDeath
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityShootBow(event: EntityShootBowEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityPickupItem(event: EntityPickupItemEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onProjectileHit(event: ProjectileHitEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityExplode(event: EntityExplodeEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerFish(event: PlayerFishEvent) = interceptBukkitEvent(event)
}