package sn.inpr.ragebaitinator.integrations

import me.clip.placeholderapi.PlaceholderAPI
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.command.CommandManager
import sn.inpr.ragebaitinator.utility.LogUtil.info
import sn.inpr.ragebaitinator.utility.PlayerUtil

object PAPI {
    val present: Boolean get() = Bukkit.getServer().pluginManager.getPlugin("PlaceholderAPI") != null

    fun parse(player: Player?, string: String): String {
        // Fallback to returning the original string if PlaceholderAPI is not present
        return if (present) {
            PlaceholderAPI.setPlaceholders(player, string)
        } else {
            info("&6A PAPI integration method was called, but PlaceholderAPI is not present. This is a bug, please report this!")
            string
        }
    }
}

class PAPIExpansion : PlaceholderExpansion() {
    val pluginVersion = Main.instance().description.version
    val pluginAuthor = Main.instance().description.authors.joinToString(", ")

    override fun getIdentifier(): String = "ragebaitinator"
    override fun getAuthor(): String = pluginAuthor
    override fun getVersion(): String = pluginVersion
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        return when (params) {
            "active" -> {
                if (player?.player == null) return null
                val active = PlayerUtil.getEnabledTrolls(player.player ?: return "err")
                if (active.isEmpty()) "None" else active.joinToString(", ")
            }
            "enabledcount" -> PlayerUtil.getEnabledTrolls(player?.player ?: return "err").size.toString()
            "disabledcount" -> (CommandManager.toggleTrolls.size - PlayerUtil.getEnabledTrolls(player?.player ?: return "err").size).toString()
            "totalcount" -> CommandManager.trolls.size.toString()
            "totaltogglecount" -> CommandManager.toggleTrolls.size.toString()
            "totaloncecount" -> CommandManager.onceTrolls.size.toString()
            else -> {
                if (player?.player == null) return null
                val toggleTroll = CommandManager.toggleTrolls.find { it.info.name == params }
                if (toggleTroll != null)
                    if (toggleTroll.hasPlayer(player.player ?: return "err")) "true" else "false"
                else null
            }
        }
    }
}