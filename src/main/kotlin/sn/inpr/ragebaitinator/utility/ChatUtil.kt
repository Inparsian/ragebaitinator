package sn.inpr.ragebaitinator.utility

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object ChatUtil {
    fun send(sender: CommandSender, message: String) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message))
    }
}