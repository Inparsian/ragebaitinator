package sn.inpr.ragebaitinator.utility

import org.bukkit.Bukkit

object LogUtil {
    fun info(message: String) {
        Bukkit.getLogger().info(AnsiUtil.translateAltColorCode('&', "&c[Ragebaitinator] &r$message&r"))
    }
}