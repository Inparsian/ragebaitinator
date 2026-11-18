package sn.inpr.ragebaitinator.gui

import sn.inpr.ragebaitinator.Main
import sn.inpr.ragebaitinator.Main.Companion.language
import sn.inpr.ragebaitinator.utility.HexUtil.hexifyAndColorCode
import sn.inpr.ragebaitinator.utility.TypeUtil

object GuiText {
    private val onlyAPlaceholder = Regex("^%([^%]+)%$")
    private val placeholder = Regex("%([^%]+)%")

    // the same ones that every other message has
    private fun builtIn(name: String): String? = when (name) {
        "prefix" -> language.prefix
        "c1" -> language.primaryColor
        "c2" -> language.secondaryColor
        "c3" -> language.tertiaryColor
        "version" -> protect(Main.instance().description.version)
        "plugin" -> protect(Main.instance().description.name)
        else -> null
    }

    // what is put into a message is shown as it is
    private const val AMPERSAND = '\uE000'
    private const val PERCENT = '\uE001'

    private fun protect(value: String): String = value.replace('&', AMPERSAND).replace('%', PERCENT)
    private fun restore(text: String): String = text.replace(AMPERSAND, '&').replace(PERCENT, '%')
    private fun fill(template: String, values: Array<out Pair<String, String>>): String
        = restore(hexifyAndColorCode(placeholder.replace(template) { match ->
            val name = match.groupValues[1]
            values.firstOrNull { it.first == name }?.second?.let { protect(it) } ?: builtIn(name) ?: match.value
        }))

    fun text(key: String, vararg values: Pair<String, String>): String = fill(language.stringOr(key, ""), values)
    fun lines(key: String, vararg values: Pair<String, String>, multiline: Map<String, List<String>> = emptyMap()): MutableList<String> {
        val raw = language.stringOr(key, "")
        val result = mutableListOf<String>()
        if (raw.isEmpty())
            return result

        for (line in raw.split("\n")) {
            val expansion = onlyAPlaceholder.matchEntire(line.trim())?.groupValues?.get(1)?.let { multiline[it] }
            if (expansion != null)
                result.addAll(expansion)
            else
                result.add(fill(line, values))
        }

        return result
    }

    fun wrap(text: String, length: Int = 50): List<String>
        = TypeUtil.splitAtLength(text, length, language.secondaryColor).map { hexifyAndColorCode(it) }
}
