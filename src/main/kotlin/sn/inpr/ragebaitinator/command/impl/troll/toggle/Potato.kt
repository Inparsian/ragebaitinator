package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.EntityDamageByEntitySubscriber
import sn.inpr.ragebaitinator.listeners.EntityDamageSubscriber
import sn.inpr.ragebaitinator.listeners.EntityPickupItemSubscriber
import sn.inpr.ragebaitinator.listeners.PacketEventReceiveSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerInteractSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerJoinSubscriber
import sn.inpr.ragebaitinator.listeners.PlayerMoveSubscriber
import sn.inpr.ragebaitinator.listeners.ProjectileLaunchSubscriber
import sn.inpr.ragebaitinator.type.PlayerSnapshot
import sn.inpr.ragebaitinator.utility.LogUtil.info
import sn.inpr.ragebaitinator.utility.Reflection
import java.util.UUID
import kotlin.collections.containsKey

@CommandInfo(
    name = "potato",
    description = "Oh hi, how are you holding up? Because I'M A POTATO.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER),
    ],
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Potato : BaseToggleTroll(),
    PlayerJoinSubscriber,
    PlayerMoveSubscriber,
    PlayerInteractSubscriber,
    EntityDamageByEntitySubscriber,
    EntityDamageSubscriber,
    EntityPickupItemSubscriber,
    ProjectileLaunchSubscriber,
    PacketEventReceiveSubscriber
{
    val potatoes: Map<UUID, Pair<Item, PlayerSnapshot>> field = mutableMapOf()

    val potatoMaterial = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
        Reflection.getMaterial("POTATO_ITEM") ?: Material.POTATO
    else
        Material.POTATO

    override fun init(sender: CommandSender, target: Player, args: Array<out String>): String {
        val potato = target.world.dropItemNaturally(target.location, ItemStack(potatoMaterial))
        potato.pickupDelay = Int.MAX_VALUE
        potato.isInvulnerable = true
        potato.velocity = target.velocity
        potato.customName = "Potahtoe" // prevent despawning

        val snapshot = PlayerSnapshot(target)
        potatoes[target.uniqueId] = Pair(potato, snapshot)
        target.inventory.clear()
        target.activePotionEffects.forEach { target.removePotionEffect(it.type) }
        Bukkit.getOnlinePlayers().filter { it != target }.forEach { it.hidePlayer(Main.plugin!!, target) }

        val camera = WrapperPlayServerCamera(potato.entityId)
        PacketEvents.getAPI().playerManager.sendPacket(target, camera)

        // tp the player so the item does not unload on their end
        addTaskTimer(target, {
            target.teleport(potato.location.add(0.0, 2.0, 0.0))
            target.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 72005, 0, false, false))
            potato.pickupDelay = Int.MAX_VALUE
        }, 0, 2, true)

        return "&c${target.name} &fis now living in a potato battery"
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        return "&c${target.name} &fis no longer living in a potato battery"
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        if (potatoes.containsKey(player.uniqueId)) {
            val [potato, snapshot] = potatoes.remove(player.uniqueId)!!
            player.teleport(potato.location.setDirection(player.location.direction))
            player.velocity = potato.velocity
            Bukkit.getOnlinePlayers().forEach { it.showPlayer(Main.plugin!!, player) }
            snapshot.restore()
            potato.remove()

            val camera = WrapperPlayServerCamera(player.entityId)
            PacketEvents.getAPI().playerManager.sendPacket(player, camera)
        }
    }

    override fun onPlayerJoin(event: PlayerJoinEvent) {
        potatoes.forEach { [player, _] -> Bukkit.getPlayer(player)?.let { event.player.hidePlayer(Main.plugin!!, it) } }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (event !is PlayerTeleportEvent && potatoes.containsKey(event.player.uniqueId))
            event.isCancelled = true
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if (potatoes.containsKey(event.player.uniqueId))
            event.isCancelled = true
    }

    override fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (event.damager is Player && potatoes.containsKey(event.damager.uniqueId))
            event.isCancelled = true
    }

    override fun onEntityDamage(event: EntityDamageEvent) {
        if (event.entity is Player && potatoes.containsKey(event.entity.uniqueId))
            event.isCancelled = true
    }

    override fun onEntityPickupItem(event: EntityPickupItemEvent) {
        if (potatoes.any { it.value.first == event.item } || event.entity is Player && potatoes.containsKey(event.entity.uniqueId))
            event.isCancelled = true
    }

    override fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val player = event.entity.shooter as? Player ?: return
        if (potatoes.containsKey(player.uniqueId))
            event.isCancelled = true
    }

    // cancel some actions at the packet level that potatoes would not be able
    // to do
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if (potatoes.containsKey(event.getPlayer())) {
            when(event.packetType) {
                PacketType.Play.Client.INTERACT_ENTITY,
                PacketType.Play.Client.ATTACK,
                PacketType.Play.Client.ANIMATION,
                PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.PLAYER_ABILITIES,
                PacketType.Play.Client.PLAYER_DIGGING,
                PacketType.Play.Client.PLAYER_INPUT,
                PacketType.Play.Client.UPDATE_SIGN,
                PacketType.Play.Client.CREATIVE_INVENTORY_ACTION,
                PacketType.Play.Client.UPDATE_STRUCTURE_BLOCK,
                PacketType.Play.Client.UPDATE_COMMAND_BLOCK,
                PacketType.Play.Client.UPDATE_COMMAND_BLOCK_MINECART,
                PacketType.Play.Client.ENTITY_ACTION -> event.isCancelled = true
            }
        }
    }
}