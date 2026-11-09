package sn.inpr.ragebaitinator.command.impl.troll.toggle

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.GameMode
import com.github.retrooper.packetevents.protocol.player.UserProfile
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying
import com.github.retrooper.packetevents.wrapper.play.server.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main.Companion.runTask
import sn.inpr.ragebaitinator.command.def.*
import sn.inpr.ragebaitinator.listeners.*
import sn.inpr.ragebaitinator.type.PlayerSnapshot
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.type.sendPacket
import sn.inpr.ragebaitinator.utility.LocationUtil
import sn.inpr.ragebaitinator.utility.Reflection
import kotlin.math.absoluteValue

@CommandInfo(
    name = "potato",
    category = CommandInfo.Category.ILLUSIONS,
    icon = Material.POTATO,
    description = "Oh hi, how are you holding up? Because I'M A POTATO.",
    attributes = [
        CommandInfo.Attribute.DEATH, CommandInfo.Attribute.LOGOUT
    ]
)
class Potato : BaseToggleTroll(),
    PlayerTeleportMonitorSubscriber,
    PlayerInteractSubscriber,
    EntityDamageSubscriber,
    EntityPickupItemSubscriber,
    PacketEventReceiveSubscriber,
    PacketEventSendSubscriber,
    TransformsPlayer
{
    companion object {
        val potatoMaterial = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
            Reflection.getMaterial("POTATO_ITEM") ?: Material.POTATO
        else
            Material.POTATO
    }

    // a potato that was removed is no longer anything to be, so the player is themselves until it's replaced
    override fun transformation(player: RbPlayer): Entity? =
        getField<Pair<Item, PlayerSnapshot>>(player, "potato")?.first?.takeIf { it.isValid }

    private fun spawnPotato(at: Location, velocity: Vector): Item {
        val potato = at.world!!.dropItemNaturally(at, ItemStack(potatoMaterial))
        potato.pickupDelay = Int.MAX_VALUE
        potato.isInvulnerable = true
        potato.velocity = velocity
        potato.customName = "Potahtoe" // prevent despawning
        return potato
    }

    private fun respawnPotato(target: RbPlayer, at: Location) {
        // it would be removed again right away
        val world = at.world ?: return
        if (at.y < world.minHeight - 64)
            return

        val snapshot = getField<Pair<Item, PlayerSnapshot>>(target, "potato")?.second ?: return
        val potato = spawnPotato(at, Vector())
        setField(target, "potato", Pair(potato, snapshot))
        target.sendPacket(WrapperPlayServerCamera(potato.entityId))
    }

    private fun sendFakeGameMode(player: RbPlayer, mode: GameMode) {
        val api = PacketEvents.getAPI()
        val bukkitPlayer = player.bukkit ?: return
        val latency = api.playerManager.getPing(bukkitPlayer)
        val profile = UserProfile(player.uuid, player.name)
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
        player.sendPacketSilently(info)
        player.sendPacketSilently(WrapperPlayServerChangeGameState(
            WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, mode.id.toFloat()))
    }

    override fun init(sender: CommandSender, target: RbPlayer, args: Args): String {
        // once the potato is registered, target.entity (and everything derived from it) is the potato, so anything
        // meant for the player itself has to go through the Bukkit player
        val player = target.bukkit ?: error("${target.name} is not online")
        val potato = spawnPotato(player.location, player.velocity)

        val snapshot = PlayerSnapshot(player)
        setField(target, "potato", Pair(potato, snapshot))
        target.clearInventory()
        target.clearPotionEffects()
        target.allowFlight = true

        // destroy this player entity for others
        val destroy = WrapperPlayServerDestroyEntities(target.bodyEntityId)
        target.others.sendPacket(destroy)

        // prevent interactions (and self-interaction kicks) by setting spectator mode client-side
        sendFakeGameMode(target, GameMode.SPECTATOR)
        target.sendPacket(WrapperPlayServerCamera(potato.entityId))

        addTaskTimer(target, {
            val [potato] = getField<Pair<Item, PlayerSnapshot>>(target, "potato") ?: return@addTaskTimer
            if (!potato.isValid) {
                respawnPotato(target, player.location)
                return@addTaskTimer
            }

            // sync the player's location with the potato's location to prevent unloading
            val playerChunk = LocationUtil.chunkify(player.location).setY(0)
            val potatoChunk = LocationUtil.chunkify(potato.location).setY(0)
            val yDistance = (player.location.y - potato.location.y).absoluteValue
            val chunkDistance = playerChunk.distance(potatoChunk)
            if (chunkDistance > 0 || yDistance > 2)
                player.teleport(potato.location)
            potato.pickupDelay = Int.MAX_VALUE
        }, 0, 2, true)

        return placeholdersOr("init", "%prefix% %c1%%player% %c2%is now living in a potato battery", target)
            .toString()
    }

    override fun deinit(sender: CommandSender, target: RbPlayer): String {
        return placeholdersOr("deinit", "%prefix% %c1%%player% %c2%is no longer living in a potato battery", target)
            .toString()
    }

    override fun onRemovePlayer(player: RbPlayer, reason: RemoveReason) {
        // taking the potato is what turns them back into a player, so this has to come before anything uses their entity
        val [potato, snapshot] = takeField<Pair<Item, PlayerSnapshot>>(player, "potato") ?: return
        snapshot.restore()
        // a potato that's gone has nowhere to bring them back from
        val destination = if (potato.isValid) potato.location else null
        val velocity = potato.velocity
        potato.remove()
        if (player.isOnline) {
            sendFakeGameMode(player, GameMode.valueOf(player.gameMode.name))
            if (destination != null) {
                player.teleport(destination.setDirection(player.location.direction))
                player.velocity = velocity
            }
            player.sendPacket(WrapperPlayServerCamera(player.bodyEntityId))
            player.others.forEach {
                it.hidePlayer(player)
                it.showPlayer(player)
            }
        }
    }

    override fun onPlayerTeleportMonitor(event: PlayerTeleportEvent) {
        val to = event.to ?: return
        val target = event.player.rb
        val [potato] = getField<Pair<Item, PlayerSnapshot>>(target, "potato") ?: return
        if (!potato.isValid)
            respawnPotato(target, to)
        else if (potato.world != to.world || potato.location.distanceSquared(to) >= 1)
            potato.teleport(to)
    }

    override fun onPlayerInteract(event: PlayerInteractEvent) = event.cancelIf(hasPlayer(event.player))
    override fun onEntityDamage(event: EntityDamageEvent) = event.cancelIf(hasPlayer(event.entity))
    override fun onEntityPickupItem(event: EntityPickupItemEvent)
        = event.cancelIf(fieldValues<Pair<Item, PlayerSnapshot>>("potato").any { it.first == event.item } || hasPlayer(event.entity))

    // cancel some actions at the packet level that potatoes would not be able to do
    override fun onPacketReceive(event: PacketReceiveEvent) {
        val uuid = event.user.uuid ?: return
        if (!hasPlayer(uuid))
            return

        if (WrapperPlayClientPlayerFlying.isFlying(event.packetType)) {
            event.isCancelled = true
            return
        }

        when (event.packetType) {
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

        // if this potato respawns as a result of a far teleport/world change, we should resync camera
        if (event.packetType == PacketType.Play.Server.SPAWN_ENTITY) {
            val potato = getField<Pair<Item, PlayerSnapshot>>(uuid, "potato")?.first
            if (potato != null && WrapperPlayServerSpawnEntity(event).entityId == potato.entityId)
                runTask {
                    val owner = Bukkit.getPlayer(uuid) ?: return@runTask
                    if (potato.isValid)
                        PacketEvents.getAPI().playerManager.sendPacket(owner, WrapperPlayServerCamera(potato.entityId))
                }
        }

        if (event.packetType == PacketType.Play.Server.SPAWN_PLAYER) { // <=1.20.1
            val id = WrapperPlayServerSpawnPlayer(event).entityId
            if (players.keys.any { it.bodyEntityId == id }) event.isCancelled = true
        } else if (event.packetType == PacketType.Play.Server.SPAWN_ENTITY) { // >=1.20.2
            val id = WrapperPlayServerSpawnEntity(event).entityId
            if (players.keys.any { it.bodyEntityId == id }) event.isCancelled = true
        }

        if (hasPlayer(uuid))
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