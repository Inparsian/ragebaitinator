package sn.inpr.ragebaitinator.utility

import org.bukkit.ChatColor

object AnsiUtil {
    private val escapes: Map<String, String> = mapOf(
        // Colors
        Pair("§0", "\u001B[30m"),
        Pair("§1", "\u001B[34m"),
        Pair("§2", "\u001B[32m"),
        Pair("§3", "\u001B[36m"),
        Pair("§4", "\u001B[31m"),
        Pair("§5", "\u001B[35m"),
        Pair("§6", "\u001B[33m"),
        Pair("§7", "\u001B[37m"),
        Pair("§8", "\u001B[90m"),
        Pair("§9", "\u001B[94m"),
        Pair("§a", "\u001B[92m"),
        Pair("§b", "\u001B[96m"),
        Pair("§c", "\u001B[91m"),
        Pair("§d", "\u001B[95m"),
        Pair("§e", "\u001B[93m"),
        Pair("§f", "\u001B[97m"),

        // Formatting
        Pair("§k", "\u001B[8m"),
        Pair("§l", "\u001B[1m"),
        Pair("§m", "\u001B[9m"),
        Pair("§n", "\u001B[4m"),
        Pair("§o", "\u001B[3m"),
        Pair("§r", "\u001B[0m")
    )

    fun translate(string: String): String {
        return escapes.entries
            .fold(string) { acc, entry -> acc.replace(entry.key, entry.value) }
    }

    fun translateAltColorCode(altCodeChar: Char, string: String): String {
        return translate(ChatColor.translateAlternateColorCodes(altCodeChar, string))
    }
}