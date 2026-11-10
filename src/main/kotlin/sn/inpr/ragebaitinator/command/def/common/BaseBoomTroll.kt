package sn.inpr.ragebaitinator.command.def.common

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import sn.inpr.ragebaitinator.command.def.BaseToggleTroll
import sn.inpr.ragebaitinator.type.RbPlayer
import sn.inpr.ragebaitinator.type.rb
import sn.inpr.ragebaitinator.utility.LocationUtil

/**
 * A toggle troll that triggers an explosion whenever a certain event happens.
 * There are two flags for persistence and destructiveness.
 */
abstract class BaseBoomTroll : BaseToggleTroll() {
    fun addBoomFlags(target: RbPlayer, persistent: Boolean, destructive: Boolean) {
        if (persistent) addFlag(target, "persistent")
        if (destructive) addFlag(target, "destructive")
    }

    fun kaboom(event: Event, player: Player, sourceLocation: Location, cancel: Boolean = true): Unit =
        kaboom(event, player.rb, sourceLocation, cancel)

    fun kaboom(event: Event, player: RbPlayer, sourceLocation: Location, cancel: Boolean = true) {
        val run = {
            if (hasFlag(player, "destructive"))
                sourceLocation.world?.createExplosion(sourceLocation, 5f, true)
            else {
                LocationUtil.createSafeExplosion(sourceLocation, 5f)
            }

            if (player.gameMode == GameMode.CREATIVE || player.gameMode == GameMode.SPECTATOR)
                player.health = 0.0

            if (!hasFlag(player, "persistent"))
                removePlayer(player)
        }

        if (cancel) event.cancelIfAndThen(hasPlayer(player), run)
        else if(hasPlayer(player)) run()
    }
}