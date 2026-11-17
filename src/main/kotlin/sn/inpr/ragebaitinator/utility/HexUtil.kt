package sn.inpr.ragebaitinator.utility

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion
import net.md_5.bungee.api.ChatColor
import kotlin.math.min
import kotlin.math.roundToInt

object HexUtil {
    private const val HEX = "#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})"
    private val regularPattern = Regex("%($HEX)%")
    private val gradientPattern = Regex("%($HEX(?:,$HEX)+)%((?:(?!§[0-9a-fA-FrRxX]|%#).)*)", RegexOption.DOT_MATCHES_ALL)
    private val hexDigits = Regex("(?:§[0-9a-fA-F]){6}")

    private fun codeLength(text: String, index: Int): Int {
        return if (text[index] != '§' || index + 1 >= text.length)
            0
        else if (text[index + 1] in "xX" && index + 14 <= text.length && hexDigits.matchesAt(text, index + 2))
            14
        else
            2
    }

    private fun rgb(hex: String): IntArray {
        val full = oneToTwo(hex)
        return intArrayOf(full.substring(1, 3).toInt(16), full.substring(3, 5).toInt(16), full.substring(5, 7).toInt(16))
    }

    fun gradient(colors: List<String>, text: String): String {
        if (colors.isEmpty())
            return text
        val stops = colors.map { rgb(it) }
        var visible = 0
        var i = 0
        while (i < text.length) {
            val length = codeLength(text, i)
            if (length == 0 && !text[i].isWhitespace()) visible++
            i += length.coerceAtLeast(1)
        }

        val out = StringBuilder()
        val formats = StringBuilder()
        var replaced = false
        var seen = 0
        i = 0
        while (i < text.length) {
            val length = codeLength(text, i)
            if (length > 0) {
                val code = text.substring(i, i + length)
                when (code[1].lowercaseChar()) {
                    'k', 'l', 'm', 'n', 'o' -> formats.append(code)
                    'r' -> { formats.setLength(0); replaced = false }
                    // a color ends all formatting, as it does anywhere else
                    else -> { formats.setLength(0); replaced = true }
                }
                out.append(code)
                i += length
                continue
            }

            val space = text[i].isWhitespace()
            if (!replaced && (!space || (formats.isNotEmpty() && text[i] != '\n'))) {
                // use linear interpolation to determine the color
                val ratio = if (visible <= 1) 0.0 else (seen.toDouble() / (visible - 1)).coerceAtMost(1.0)
                val position = ratio * (stops.size - 1)
                val from = min(position.toInt(), (stops.size - 2).coerceAtLeast(0))
                val to = min(from + 1, stops.size - 1)
                val between = position - from
                val [r, g, b] = (0..2).map { (stops[from][it] * (1.0 - between) + stops[to][it] * between).roundToInt() }
                out.append(ChatColor.of(String.format("#%02x%02x%02x", r, g, b))).append(formats)
            }
            out.append(text[i])
            if (!space) seen++
            i++
        }
        return out.toString()
    }

    fun oneToTwo(input: String): String {
        return if (input.length != 4) input
        else "#${input[1]}${input[1]}${input[2]}${input[2]}${input[3]}${input[3]}"
    }

    fun hexify(input: String): String {
        if (PacketEvents.getAPI().serverManager.version.isOlderThan(ServerVersion.V_1_16))
            return input
        val gradients = gradientPattern.replace(input) { gradient(it.groupValues[1].split(","), it.groupValues[2]) }
        return regularPattern.replace(gradients) { ChatColor.of(oneToTwo(it.groupValues[1])).toString() }
    }

    fun hexifyAndColorCode(altColorChar: Char, textToTranslate: String) = hexify(ChatColor.translateAlternateColorCodes(altColorChar, textToTranslate) ?: textToTranslate)
    fun hexifyAndColorCode(textToTranslate: String) = hexifyAndColorCode('&', textToTranslate)
}