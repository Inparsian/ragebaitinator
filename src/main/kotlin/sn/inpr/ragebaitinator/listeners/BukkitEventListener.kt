package sn.inpr.ragebaitinator.listeners

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.FurnaceStartSmeltEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.*
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.CommandInfo

fun interface PlayerJoinSubscriber { fun onPlayerJoin(event: PlayerJoinEvent) }
fun interface PlayerQuitSubscriber { fun onPlayerQuit(event: PlayerQuitEvent) }
fun interface AsyncPlayerChatSubscriber { fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) }
fun interface PlayerTeleportSubscriber { fun onPlayerTeleport(event: PlayerTeleportEvent) }
fun interface PlayerMoveSubscriber { fun onPlayerMove(event: PlayerMoveEvent) }
fun interface PlayerVelocitySubscriber { fun onPlayerVelocity(event: PlayerVelocityEvent) }
fun interface PlayerInteractSubscriber { fun onPlayerInteract(event: PlayerInteractEvent) }
fun interface BlockBreakSubscriber { fun onBlockBreak(event: BlockBreakEvent) }
fun interface BlockPlaceSubscriber { fun onBlockPlace(event: BlockPlaceEvent) }
fun interface PlayerBucketEmptySubscriber { fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) }
fun interface PlayerBucketFillSubscriber { fun onPlayerBucketFill(event: PlayerBucketFillEvent) }
fun interface EntityDamageByEntitySubscriber { fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) }
fun interface EntityDamageSubscriber { fun onEntityDamage(event: EntityDamageEvent) }
fun interface PlayerDeathSubscriber { fun onPlayerDeath(event: PlayerDeathEvent) }
fun interface EntityDeathSubscriber { fun onEntityDeath(event: EntityDeathEvent) }
fun interface EntityRegainHealthSubscriber { fun onEntityRegainHealth(event: EntityRegainHealthEvent) }
fun interface EntityResurrectSubscriber { fun onEntityResurrect(event: EntityResurrectEvent) }
fun interface PlayerRespawnSubscriber { fun onPlayerRespawn(event: PlayerRespawnEvent) }
fun interface EntityShootBowSubscriber { fun onEntityShootBow(event: EntityShootBowEvent) }
fun interface EntityPickupItemSubscriber { fun onEntityPickupItem(event: EntityPickupItemEvent) }
fun interface ProjectileLaunchSubscriber { fun onProjectileLaunch(event: ProjectileLaunchEvent) }
fun interface ProjectileHitSubscriber { fun onProjectileHit(event: ProjectileHitEvent) }
fun interface EntityExplodeSubscriber { fun onEntityExplode(event: EntityExplodeEvent) }
fun interface PlayerItemConsumeSubscriber { fun onPlayerItemConsume(event: PlayerItemConsumeEvent) }
fun interface EntityToggleGlideSubscriber { fun onEntityToggleGlide(event: EntityToggleGlideEvent) }
fun interface PlayerBedEnterSubscriber { fun onPlayerBedEnter(event: PlayerBedEnterEvent) }
fun interface PlayerFishSubscriber { fun onPlayerFish(event: PlayerFishEvent) }
fun interface PlayerToggleSneakSubscriber { fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) }
fun interface PlayerExpChangeSubscriber { fun onPlayerExpChange(event: PlayerExpChangeEvent) }
fun interface PlayerCommandPreprocessSubscriber { fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) }
fun interface PlayerItemHeldSubscriber { fun onPlayerItemHeld(event: PlayerItemHeldEvent) }
fun interface PlayerSwapHandItemsSubscriber { fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) }
fun interface InventoryClickSubscriber { fun onInventoryClick(event: InventoryClickEvent) }
fun interface InventoryDragSubscriber { fun onInventoryDrag(event: InventoryDragEvent) }
fun interface PlayerItemDamageSubscriber { fun onPlayerItemDamage(event: PlayerItemDamageEvent) }
fun interface PotionSplashSubscriber { fun onPotionSplash(event: PotionSplashEvent) }
fun interface AreaEffectCloudApplySubscriber { fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) }
fun interface FurnaceStartSmeltSubscriber { fun onFurnaceStartSmelt(event: FurnaceStartSmeltEvent) }
fun interface PlayerDropItemSubscriber { fun onPlayerDropItem(event: PlayerDropItemEvent) }
fun interface EntityChangeBlockSubscriber { fun onEntityChangeBlock(event: EntityChangeBlockEvent) }

object BukkitEventListener : Listener {
    fun removeToggleTrollsWithAttribute(player: Player, attribute: CommandInfo.Attribute, reason: String) {
        CommandManager.toggleTrolls
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
            if (command is PlayerBucketEmptySubscriber && event is PlayerBucketEmptyEvent) command.onPlayerBucketEmpty(event)
            if (command is PlayerBucketFillSubscriber && event is PlayerBucketFillEvent) command.onPlayerBucketFill(event)
            if (command is EntityDamageByEntitySubscriber && event is EntityDamageByEntityEvent) command.onEntityDamageByEntity(event)
            if (command is EntityDamageSubscriber && event is EntityDamageEvent) command.onEntityDamage(event)
            if (command is PlayerDeathSubscriber && event is PlayerDeathEvent) command.onPlayerDeath(event)
            if (command is EntityDeathSubscriber && event is EntityDeathEvent) command.onEntityDeath(event)
            if (command is EntityRegainHealthSubscriber && event is EntityRegainHealthEvent) command.onEntityRegainHealth(event)
            if (command is EntityResurrectSubscriber && event is EntityResurrectEvent) command.onEntityResurrect(event)
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
            if (command is PlayerToggleSneakSubscriber && event is PlayerToggleSneakEvent) command.onPlayerToggleSneak(event)
            if (command is PlayerExpChangeSubscriber && event is PlayerExpChangeEvent) command.onPlayerExpChange(event)
            if (command is PlayerCommandPreprocessSubscriber && event is PlayerCommandPreprocessEvent) command.onPlayerCommandPreprocess(event)
            if (command is PlayerItemHeldSubscriber && event is PlayerItemHeldEvent) command.onPlayerItemHeld(event)
            if (command is PlayerSwapHandItemsSubscriber && event is PlayerSwapHandItemsEvent) command.onPlayerSwapHandItems(event)
            if (command is InventoryClickSubscriber && event is InventoryClickEvent) command.onInventoryClick(event)
            if (command is InventoryDragSubscriber && event is InventoryDragEvent) command.onInventoryDrag(event)
            if (command is PlayerItemDamageSubscriber && event is PlayerItemDamageEvent) command.onPlayerItemDamage(event)
            if (command is PotionSplashSubscriber && event is PotionSplashEvent) command.onPotionSplash(event)
            if (command is AreaEffectCloudApplySubscriber && event is AreaEffectCloudApplyEvent) command.onAreaEffectCloudApply(event)
            if (command is FurnaceStartSmeltSubscriber && event is FurnaceStartSmeltEvent) command.onFurnaceStartSmelt(event)
            if (command is PlayerDropItemSubscriber && event is PlayerDropItemEvent) command.onPlayerDropItem(event)
            if (command is EntityChangeBlockSubscriber && event is EntityChangeBlockEvent) command.onEntityChangeBlock(event)
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
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) = interceptBukkitEvent(event) // EntityDamageByEntity & EntityDamage
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeath(event: EntityDeathEvent) = interceptBukkitEvent(event) // EntityDeath & PlayerDeath
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityResurrect(event: EntityResurrectEvent) = interceptBukkitEvent(event)
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
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerExpChange(event: PlayerExpChangeEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPotionSplash(event: PotionSplashEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onAreaEffectCloudApply(event: AreaEffectCloudApplyEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFurnaceStartSmelt(event: FurnaceStartSmeltEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDropItem(event: PlayerDropItemEvent) = interceptBukkitEvent(event)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) = interceptBukkitEvent(event)
}