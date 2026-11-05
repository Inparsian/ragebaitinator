package sn.inpr.ragebaitinator.command.def

import org.bukkit.entity.Player
import org.bukkit.event.player.AsyncPlayerChatEvent
import sn.inpr.ragebaitinator.listeners.AsyncPlayerChatSubscriber

/**
 * A toggle troll that rewrites everything its targets say. The ones using this are all incompatible with
 * each other, since stacking them would just be unreadable noise instead of a specific bit.
 */
abstract class BaseChatTroll : BaseToggleTroll(), AsyncPlayerChatSubscriber {
    abstract fun transform(player: Player, message: String): String

    override fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        if (!event.isCancelled && hasPlayer(event.player))
            event.message = transform(event.player, event.message)
    }
}
