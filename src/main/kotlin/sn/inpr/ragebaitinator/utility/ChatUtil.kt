package sn.inpr.ragebaitinator.utility

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import sn.inpr.ragebaitinator.integrations.PAPI
import sn.inpr.ragebaitinator.type.Placeholders

object ChatUtil {
    fun send(sender: CommandSender, target: Player?, message: String) {
        var message = ChatColor.translateAlternateColorCodes('&', message)
        if (PAPI.present)
            message = PAPI.parse(target, message)
        sender.sendMessage(message)
    }

    fun send(sender: CommandSender, target: Player?, placeholders: Placeholders) = send(sender, target, placeholders.toString())
    fun send(sender: CommandSender, message: String) = send(sender, null, message)
    fun send(sender: CommandSender, placeholders: Placeholders) = send(sender, null, placeholders.toString())
}