package sn.inpr.ragebaitinator.type

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import org.bukkit.*
import org.bukkit.block.data.BlockData
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.BaseTroll
import sn.inpr.ragebaitinator.utility.LocationUtil
import java.util.*

/** Shorthand for converting a Bukkit player to a Ragebaitinator player */
val Player.rb: RbPlayer get() = RbPlayer(this)

/** The Ragebaitinator player this entity is, or null if it isn't a player */
val Entity.asRb: RbPlayer? get() = (this as? Player)?.rb

/** The Ragebaitinator player who shot this, or null if it wasn't shot by a player */
val Projectile.shooterRb: RbPlayer? get() = (shooter as? Player)?.rb

/** Everyone in this world */
val World.rbPlayers: List<RbPlayer> get() = players.map { it.rb }

fun Iterable<RbPlayer>.sendPacket(vararg packets: PacketWrapper<*>) = forEach { it.sendPacket(*packets) }

/** A Ragebaitinator player */
@JvmInline
value class RbPlayer(val uuid: UUID) {
    companion object {
        /** How far away somebody can be and still see a player, as far as anything sent to the ones who can goes */
        const val VIEW_RANGE = 128.0

        val online: List<RbPlayer> get() = Bukkit.getOnlinePlayers().map { it.rb }
    }

    constructor(player: Player) : this(player.uniqueId)

    /** The PacketEvents user. In most cases, you shouldn't need to access this directly. */
    val packetEventsUser: User? get() = PacketEvents.getAPI().playerManager.getUser(bukkit ?: return null)

    // Helpers for PacketEvents
    fun sendPacket(packet: PacketWrapper<*>) = packetEventsUser?.sendPacket(packet)
    /** Sends every packet in the specified list in the order they are specified */
    fun sendPacket(vararg packets: PacketWrapper<*>) = packets.forEach { sendPacket(it) }
    fun sendPacketSilently(packet: PacketWrapper<*>) = packetEventsUser?.sendPacketSilently(packet)
    /** Sends every packet silently in the specified list in the order they are specified */
    fun sendPacketSilently(vararg packets: PacketWrapper<*>) = packets.forEach { sendPacketSilently(it) }

    /**
     * The underlying Bukkit player. Fall back to this if there is no equivalent
     * for the property you wish to access/modify
     *
     * This may be null if the player is not online
     */
    val bukkit: Player? get() = Bukkit.getPlayer(uuid)
    val isOnline: Boolean get() = bukkit != null

    /** Everyone else online, except this player. */
    val others: List<RbPlayer> get() = Bukkit.getOnlinePlayers().filter { it.uniqueId != this.uuid }.map { RbPlayer(it) }

    /** Gets this player's entity. If this player was transformed, it will return that entity */
    val entity: Entity? get() {
        for (transformer in CommandManager.transformers)
            transformer.transformation(this)?.let { return it }
        return bukkit
    }

    // Bukkit helpers
    val name: String get() = bukkit?.name ?: "Unknown"
    val entityId: Int get() = entity?.entityId ?: -1
    /** The id of the player's own entity. That is not [entityId] while they are transformed into something else. */
    val bodyEntityId: Int get() = bukkit?.entityId ?: -1
    val world: World get() = entity?.world ?: Bukkit.getWorlds().first()
    val location: Location get() = entity?.location ?: Location(world, 0.0, 0.0, 0.0)
    var velocity: Vector
        get() = entity?.velocity ?: Vector(0.0, 0.0, 0.0)
        set(value) {
            entity?.velocity = value
        }

    fun teleport(location: Location): Boolean? = bukkit?.teleport(location)
    /** Teleports this player whilst retaining its velocity */
    fun teleportRetainVelocity(location: Location) {
        val oldVelocity = velocity
        teleport(location)
        velocity = oldVelocity
    }
    // Vitals. These always refer to the real player, even when it is transformed, and are inert while offline
    var health: Double
        get() = bukkit?.health ?: 0.0
        set(value) { bukkit?.health = value }
    var foodLevel: Int
        get() = bukkit?.foodLevel ?: 0
        set(value) { bukkit?.foodLevel = value }
    var saturation: Float
        get() = bukkit?.saturation ?: 0f
        set(value) { bukkit?.saturation = value }
    var fireTicks: Int
        get() = bukkit?.fireTicks ?: 0
        set(value) { bukkit?.fireTicks = value }
    var fallDistance: Float
        get() = bukkit?.fallDistance ?: 0f
        set(value) { bukkit?.fallDistance = value }
    /** Offline players count as dead */
    val isDead: Boolean get() = bukkit?.isDead ?: true
    val isInvulnerable: Boolean get() = bukkit?.isInvulnerable == true
    fun damage(amount: Double, source: DamageSource) = bukkit?.damage(amount, source)
    fun damage(amount: Double, source: Entity? = null) {
        if (source != null) bukkit?.damage(amount, source) else bukkit?.damage(amount)
    }

    // Movement & abilities
    var flySpeed: Float
        get() = bukkit?.flySpeed ?: 0.1f
        set(value) { bukkit?.flySpeed = value }
    var walkSpeed: Float
        get() = bukkit?.walkSpeed ?: 0.2f
        set(value) { bukkit?.walkSpeed = value }
    var allowFlight: Boolean
        get() = bukkit?.allowFlight == true
        set(value) { bukkit?.allowFlight = value }
    var isFlying: Boolean
        get() = bukkit?.isFlying == true
        set(value) { bukkit?.isFlying = value }
    var isGliding: Boolean
        get() = bukkit?.isGliding == true
        set(value) { bukkit?.isGliding = value }
    var gameMode: GameMode
        get() = bukkit?.gameMode ?: GameMode.SURVIVAL
        set(value) { bukkit?.gameMode = value }
    val isSwimming: Boolean get() = bukkit?.isSwimming == true
    val isSneaking: Boolean get() = bukkit?.isSneaking == true
    val isInsideVehicle: Boolean get() = bukkit?.isInsideVehicle == true
    val isOnGround: Boolean get() = bukkit?.isOnGround == true

    // Perspective
    val eyeHeight: Double get() = bukkit?.eyeHeight ?: 0.0
    val eyeLocation: Location get() = bukkit?.eyeLocation ?: location
    var compassTarget: Location
        get() = bukkit?.compassTarget ?: world.spawnLocation
        set(value) { bukkit?.compassTarget = value }

    // Per-player time
    val playerTime: Long get() = bukkit?.playerTime ?: 0L
    val playerTimeOffset: Long get() = bukkit?.playerTimeOffset ?: 0L
    fun setPlayerTime(time: Long, relative: Boolean) = bukkit?.setPlayerTime(time, relative)
    fun resetPlayerTime() = bukkit?.resetPlayerTime()

    // Inventory
    var contents: Array<ItemStack?>
        get() = bukkit?.inventory?.contents ?: emptyArray()
        set(value) { bukkit?.inventory?.contents = value }
    var storageContents: Array<ItemStack?>
        get() = bukkit?.inventory?.storageContents ?: emptyArray()
        set(value) { bukkit?.inventory?.storageContents = value }
    val itemInOffHand: ItemStack get() = bukkit?.inventory?.itemInOffHand ?: ItemStack(Material.AIR)
    val helmet: ItemStack? get() = bukkit?.inventory?.helmet
    fun clearInventory() = bukkit?.inventory?.clear()
    fun removeFromInventory(item: ItemStack) = bukkit?.inventory?.remove(item)
    fun openInventory(inventory: Inventory) = bukkit?.openInventory(inventory)

    // Potion effects
    val activePotionEffects: Collection<PotionEffect> get() = bukkit?.activePotionEffects ?: emptyList()
    fun removePotionEffect(type: PotionEffectType) = bukkit?.removePotionEffect(type)
    fun clearPotionEffects() = activePotionEffects.forEach { removePotionEffect(it.type) }

    // Client-side effects
    fun playSound(location: Location, sound: String, volume: Float, pitch: Float) = bukkit?.playSound(location, sound, volume, pitch)
    fun playSound(location: Location, sound: Sound, category: SoundCategory, volume: Float, pitch: Float) =
        bukkit?.playSound(location, sound, category, volume, pitch)
    fun sendBlockChange(location: Location, data: BlockData) = bukkit?.sendBlockChange(location, data)

    // Visibility
    /** Whether [viewer] is able to see this player */
    fun isVisibleTo(viewer: Player): Boolean = bukkit?.let { viewer.canSee(it) } == true
    /**
     * Everyone who can see this player: those in the same world who this player isn't hidden from and who are within
     * [range] blocks. The player themself is one of them unless [includeSelf] is false.
     */
    fun viewers(includeSelf: Boolean = true, range: Double = VIEW_RANGE): List<RbPlayer> {
        val location = location
        val rangeSquared = range * range
        return world.players.filter { viewer ->
            if (viewer.uniqueId == uuid) includeSelf
            else isVisibleTo(viewer) && viewer.location.distanceSquared(location) <= rangeSquared
        }.map { it.rb }
    }

    /** Everything within [radius] blocks of this player, except for the player (and what they were transformed into) */
    fun nearbyEntities(radius: Double): List<Entity> {
        val own = bukkit
        val transformed = entity
        return LocationUtil.getEntitiesWithinRadius(location, radius).filter { it != own && it != transformed }
    }

    /** Hides [other] from this player */
    fun hidePlayer(other: RbPlayer) {
        other.bukkit?.let { bukkit?.hidePlayer(Main.instance(), it) }
    }
    /** Shows [other] to this player */
    fun showPlayer(other: RbPlayer) {
        other.bukkit?.let { bukkit?.showPlayer(Main.instance(), it) }
    }

    /** Whether this player is at the bottom of the world or lower, [offset] blocks aside */
    fun isInVoid(offset: Double = 0.0): Boolean = location.y <= LocationUtil.downHeightLimit + offset

    fun hasPermission(permission: String) = bukkit?.hasPermission(permission) == true
    fun isImmune(troll: BaseTroll) = hasPermission("ragebaitinator.immune.${troll.info.name}")

    override fun toString() = "RbPlayer(${name})"
}