package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.play.server.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.command.def.CommandSyntax
import sn.inpr.ragebaitinator.listeners.*
import sn.inpr.ragebaitinator.type.PlayerSnapshot
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.Reflection
import java.util.*
import kotlin.math.absoluteValue

@CommandInfo(
    name = "potato",
    description = "Oh hi, how are you holding up? Because I'M A POTATO.",
    syntax = [
        CommandSyntax(CommandSyntax.Type.PLAYER, optional = false),
    ],
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Potato : BaseToggleTroll(),
    PlayerMoveSubscriber,
    PlayerInteractSubscriber,
    EntityDamageSubscriber,
    EntityPickupItemSubscriber,
    PacketEventReceiveSubscriber,
    PacketEventSendSubscriber
{
    companion object {
        val potatoMaterial = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
            Reflection.getMaterial("POTATO_ITEM") ?: Material.POTATO
        else
            Material.POTATO
    }

    val potatoes: Map<UUID, Pair<Item, PlayerSnapshot>> field = mutableMapOf()

    private fun sendFakeGameMode(player: Player, mode: GameMode) {
        val api = PacketEvents.getAPI()
        val latency = api.playerManager.getPing(player)
        val user = api.playerManager.getUser(player) ?: return
        val profile = UserProfile(player.uniqueId, player.name)
        val info = if (api.serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_19_3))
            WrapperPlayServerPlayerInfoUpdate(
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile, true, latency, mode, null, null)
            )
        else
            WrapperPlayServerPlayerInfo(
                WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE,
                WrapperPlayServerPlayerInfo.PlayerData(null, profile, mode, latency)
            )
        user.sendPacketSilently(info)
        user.sendPacketSilently(WrapperPlayServerChangeGameState(
            WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, mode.id.toFloat()))
    }

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
        target.allowFlight = true

        // destroy this player entity for others
        val destroy = WrapperPlayServerDestroyEntities(target.entityId)
        Bukkit.getOnlinePlayers().filter { it != target }
            .forEach { PacketEvents.getAPI().playerManager.sendPacket(it, destroy) }

        // prevent interactions (and self-interaction kicks) by setting spectator mode client-side
        sendFakeGameMode(target, GameMode.SPECTATOR)
        val camera = WrapperPlayServerCamera(potato.entityId)
        PacketEvents.getAPI().playerManager.sendPacket(target, camera)

        addTaskTimer(target, {
            // sync the player's location with the potato's location to prevent unloading
            val playerChunk = LocationUtil.chunkify(target.location).setY(0)
            val potatoChunk = LocationUtil.chunkify(potato.location).setY(0)
            val yDistance = (target.location.y - potato.location.y).absoluteValue
            val chunkDistance = playerChunk.distance(potatoChunk)
            if (chunkDistance > 0 || yDistance > 2)
                target.teleport(potato.location)
            potato.pickupDelay = Int.MAX_VALUE
        }, 0, 2, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now living in a potato battery")
            .put("player", target.name)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: Player): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer living in a potato battery")
            .put("player", target.name)
            .toString()
    }

    override fun onRemovePlayer(player: Player, reason: String) {
        if (potatoes.containsKey(player.uniqueId)) {
            val [potato, snapshot] = potatoes.remove(player.uniqueId)!!
            snapshot.restore()
            potato.remove()
            if (player.isOnline) {
                sendFakeGameMode(player, GameMode.valueOf(player.gameMode.name))
                player.teleport(potato.location.setDirection(player.location.direction))
                player.velocity = potato.velocity
                val camera = WrapperPlayServerCamera(player.entityId)
                PacketEvents.getAPI().playerManager.sendPacket(player, camera)
                Bukkit.getOnlinePlayers().filter { it != player }.forEach {
                    it.hidePlayer(Main.instance(), player)
                    it.showPlayer(Main.instance(), player)
                }
            }
        }
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        if (event !is PlayerTeleportEvent && potatoes.containsKey(event.player.uniqueId))
            event.isCancelled = true
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) {
        if (potatoes.containsKey(event.player.uniqueId))
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

    // cancel some actions at the packet level that potatoes would not be able to do
    override fun onPacketReceive(event: PacketReceiveEvent) {
        val uuid = event.user.uuid ?: return
        if (potatoes.containsKey(uuid))
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
                PacketType.Play.Client.SPECTATE,
                PacketType.Play.Client.SPECTATE_ENTITY,
                PacketType.Play.Client.ENTITY_ACTION -> event.isCancelled = true
            }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        if (event.packetType == PacketType.Play.Server.SPAWN_PLAYER) { // <=1.20.1
            val id = WrapperPlayServerSpawnPlayer(event).entityId
            if (potatoes.keys.any { Bukkit.getPlayer(it)?.entityId == id }) event.isCancelled = true
        } else if (event.packetType == PacketType.Play.Server.SPAWN_ENTITY) { // >=1.20.2
            val id = WrapperPlayServerSpawnEntity(event).entityId
            if (potatoes.keys.any { Bukkit.getPlayer(it)?.entityId == id }) event.isCancelled = true
        }

        if (potatoes.containsKey(uuid))
            when (event.packetType) {
                // the client must never learn it can now fly if they were not able to before
                PacketType.Play.Server.PLAYER_ABILITIES -> event.isCancelled = true
                // also cancel game mode changes
                PacketType.Play.Server.CHANGE_GAME_STATE -> {
                    val wrapper = WrapperPlayServerChangeGameState(event)
                    if (wrapper.reason == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE)
                        event.isCancelled = true
                }
                PacketType.Play.Server.PLAYER_INFO_UPDATE -> {
                    val wrapper = WrapperPlayServerPlayerInfoUpdate(event)
                    if (WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE in wrapper.actions)
                        wrapper.entries.filter { it.gameProfile?.uuid == uuid }.forEach { it.gameMode = GameMode.SPECTATOR }
                }
                PacketType.Play.Server.PLAYER_INFO -> { // legacy, before 1.19.3
                    val wrapper = WrapperPlayServerPlayerInfo(event)
                    wrapper.playerDataList.filter { it.userProfile?.uuid == uuid }.forEach { it.gameMode = GameMode.SPECTATOR }
                }
                PacketType.Play.Server.RESPAWN -> {
                    val wrapper = WrapperPlayServerRespawn(event)
                    wrapper.gameMode = GameMode.SPECTATOR
                }
            }
    }
}