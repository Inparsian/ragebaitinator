package sn.inpr.ragebaitinator

import org.bukkit.Bukkit
import sn.inpr.ragebaitinator.utility.AnsiUtil.translateAltColorCode

object Log {
    const val PREFIX = "%#f33%[Ragebaitinator]"

    fun info(message: String) = Bukkit.getLogger().info(translateAltColorCode('&', "$PREFIX &r$message&r"))
}