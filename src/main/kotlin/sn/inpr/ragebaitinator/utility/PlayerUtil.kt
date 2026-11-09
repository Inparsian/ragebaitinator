@file:Suppress("UsePropertyAccessSyntax")

package sn.inpr.ragebaitinator.utility

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.world.damagetype.DamageTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDamageEvent
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.permissions.Permissible
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.command.def.CommandInfo
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.type.rbPlayers
import sn.inpr.ragebaitinator.type.sendPacket
import sn.inpr.ragebaitinator.utility.LocationUtil.downHeightLimit

object PlayerUtil {
    // the maximum double that you can damage a player (i think). any higher will cause a weird glitch
    // that nullifies player damage until the server is restarted.
    const val MAX_POSSIBLE_DAMAGE: Double = 170_141_173_319_264_430_000_000_000_000_000_000_000.0

    fun inVoid(player: Player, offset: Double = 0.0): Boolean = inVoid(player.rb, offset)
    fun inVoid(player: RbPlayer, offset: Double = 0.0): Boolean = player.location.y <= downHeightLimit + offset

    fun getEnabledTrolls(player: Player): MutableList<String> = getEnabledTrolls(player.rb)

    fun getEnabledTrolls(player: RbPlayer): MutableList<String> {
        val enabled: MutableList<String> = mutableListOf()
        for (cmd in CommandManager.toggleTrolls) {
            val info: CommandInfo = cmd.info
            if (cmd.hasPlayer(player))
                enabled.add(info.name)
        }
        return enabled
    }

    fun getHead(name: String): ItemStack {
        val head = if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
            // This is not deprecated <1.13
            @Suppress("DEPRECATION")
            ItemStack(Reflection.getMaterial("SKULL_ITEM") ?: return ItemStack(Material.BARRIER), 1, 3.toShort())
        else
            ItemStack(Material.PLAYER_HEAD, 1)
        val meta: SkullMeta = head.itemMeta as SkullMeta
        // IGNORE DEPRECATION WARNING!!! setOwnerPlayer fetches skins sync!!!
        @Suppress("DEPRECATION")
        meta.owner = name
        meta.setDisplayName("§f$name")
        head.itemMeta = meta
        return head
    }

    fun getHead(player: OfflinePlayer): ItemStack {
        val name = player.name ?: return ItemStack(Material.BARRIER)
        return getHead(name)
    }

    fun isFireworkBoost(event: PlayerInteractEvent): Boolean {
        return if (event.player.isGliding && event.action == Action.RIGHT_CLICK_AIR) {
            val material = if(PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_13))
                Reflection.getMaterial("FIREWORK") ?: return false
            else
                Material.FIREWORK_ROCKET
            event.item?.type == material
        } else false
    }

    // works even without a source
    fun targetPlayers(input: String, source: Permissible? = null): List<Player> {
        val online = Bukkit.getOnlinePlayers()
        return input.split(",").flatMap { token ->
            when {
                // all players (excl. sender)
                token.equals("@a", true) || token.equals("*", true) ->
                    online.filter { it != source }

                // random player (incl. sender)
                token.equals("@r", true) || token.equals("?", true) ->
                    listOfNotNull(online.randomOrNull())

                // self
                token.equals("@s", true) || token.equals("!", true) ->
                    listOfNotNull(source as? Player)

                // all players in a world
                token.startsWith("world=") -> {
                    val world = token.removePrefix("world=")
                    online.filter { it != source && it.world.name.equals(world, true) }
                }

                // all players in X radius from sender, excl. sender
                token.startsWith("radius=") -> {
                    val radius = token.removePrefix("radius=").toIntOrNull() ?: 0
                    if (source !is Player)
                        emptyList()
                    else online.filter {
                        it != source && it.location.distance(source.location) <= radius
                    }
                }

                // specific player
                else -> listOfNotNull(Bukkit.getPlayer(token))
            }
        }
    }

    // my method of "damaging" players that bypasses creative, godmode, etc. and has only a one-tick cooldown
    fun packetDamage(rbPlayer: RbPlayer, damage: Double, local: Boolean) {
        val player = rbPlayer.bukkit ?: return
        if (player.health <= 0)
            return
        val fatal = player.health - damage <= 0
        player.health = (player.health - damage).coerceAtLeast(0.0)

        // all we need for post-1.19.4 is damage event, which is far more reliable
        // than entity status packets
        if (PacketEvents.getAPI().serverManager.version.isNewerThanOrEquals(ServerVersion.V_1_19_4)) {
            val damageEventPacket = WrapperPlayServerDamageEvent(
                player.entityId,
                DamageTypes.FALL, // sure
                0, 0, null
            )

            if (local)
                rbPlayer.sendPacket(damageEventPacket)
            else player.world.rbPlayers.sendPacket(damageEventPacket)
        } else {
            // the notchian server sends both animation & entity status packets
            val animationPacket = WrapperPlayServerEntityAnimation(player.entityId, WrapperPlayServerEntityAnimation.EntityAnimationType.HURT)

            // issues can occur if we send status 3 and the death gets canceled.
            // i'm not going to bother with checking if the death gets canceled for now, so we'll have to
            // deal with this possibly looking weird for the time being
            val statusPacket = WrapperPlayServerEntityStatus(player.entityId, 2)

            // entity status 2 & 3 do play the hurt/death sounds, but only for the player being damaged
            if (!local)
                for (p in Bukkit.getOnlinePlayers()) {
                    if (p.uniqueId == rbPlayer.uuid) continue

                    p.playSound(player.location, "entity.player.${if (fatal) "death" else "hurt"}", 1f, 1f)
                }

            if (local)
                rbPlayer.sendPacket(animationPacket, statusPacket)
            else
                player.world.rbPlayers.sendPacket(statusPacket, animationPacket)
        }
    }
}